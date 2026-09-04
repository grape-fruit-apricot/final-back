package com.kh.midpoint.external.kakao;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.kh.midpoint.common.exception.ExternalApiException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.tmap.RoutePointDto;

import tools.jackson.databind.JsonNode;

@Component
public class KakaoTransitClient {

	private final RestClient transitClient;

	public KakaoTransitClient(@Value("${kakao.rest-api-key}") String kakaoRestApiKey,
			@Value("${kakao.transit.url}") String transitUrl,
			@Value("${external.timeout.connect}") long connectTimeoutMillis,
			@Value("${external.timeout.read}") long readTimeoutMillis) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMillis));
		requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMillis));

		this.transitClient = RestClient.builder()
				.baseUrl(transitUrl)
				.defaultHeader("Authorization", "KakaoAK " + kakaoRestApiKey)
				.requestFactory(requestFactory)
				.build();
	}

	public TransitRouteResponseDto findTransitRoute(Double startLng, Double startLat,
			Double endLng, Double endLat) {
		JsonNode response;
		try {
			response = transitClient.get()
					.uri(uriBuilder -> uriBuilder
							.queryParam("start_x", startLng)
							.queryParam("start_y", startLat)
							.queryParam("end_x", endLng)
							.queryParam("end_y", endLat)
							.build())
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientResponseException e) {
			throw new ExternalApiException("카카오 대중교통 요청 실패(status="
					+ e.getStatusCode().value() + ")");
		} catch (RestClientException e) {
			throw new ExternalApiException("카카오 대중교통 요청 실패: " + e.getMessage());
		}

		return toTransitRoute(response);
	}

	private TransitRouteResponseDto toTransitRoute(JsonNode response) {
		if (response == null) {
			throw new ExternalApiException("카카오 대중교통 응답을 받지 못했습니다.");
		}

		JsonNode routes = response.path("routes");
		if (!"OK".equals(response.path("status").asString()) || !routes.isArray()
				|| routes.isEmpty()) {
			throw new NotFoundException("두 지점을 잇는 대중교통 경로를 찾지 못했습니다.");
		}

		JsonNode shortestRoute = findShortestRoute(routes);
		int timeSeconds = shortestRoute.path("properties").path("totalTime").asInt(-1);
		if (timeSeconds < 0) {
			throw new ExternalApiException("카카오 대중교통 응답에 소요시간이 없습니다.");
		}

		int timeMinutes = (int) Math.ceil(timeSeconds / 60.0);
		List<RoutePointDto> points = findRoutePointList(shortestRoute);
		if (points.isEmpty()) {
			throw new NotFoundException("대중교통 경로 좌표를 찾지 못했습니다.");
		}

		return new TransitRouteResponseDto(timeMinutes, points);
	}

	private JsonNode findShortestRoute(JsonNode routes) {
		JsonNode shortestRoute = null;
		int shortestTimeSeconds = Integer.MAX_VALUE;

		for (JsonNode route : routes) {
			int timeSeconds = route.path("properties").path("totalTime").asInt(-1);
			if (timeSeconds >= 0 && timeSeconds < shortestTimeSeconds) {
				shortestTimeSeconds = timeSeconds;
				shortestRoute = route;
			}
		}

		if (shortestRoute == null) {
			throw new ExternalApiException("카카오 대중교통 응답에 경로가 없습니다.");
		}
		return shortestRoute;
	}

	private List<RoutePointDto> findRoutePointList(JsonNode route) {
		List<RoutePointDto> points = new ArrayList<>();
		for (JsonNode step : route.path("steps")) {
			for (JsonNode point : step.path("path").path("points")) {
				if (point.isArray() && point.size() >= 2) {
					points.add(new RoutePointDto(
							point.get(1).asDouble(), point.get(0).asDouble()));
				}
			}
		}
		return points;
	}

}

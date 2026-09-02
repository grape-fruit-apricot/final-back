package com.kh.midpoint.external.tmap;

import com.kh.midpoint.common.exception.ExternalApiException;
import com.kh.midpoint.common.exception.NotFoundException;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TmapRouteClient {

	private static final String START_NAME = "출발";
	private static final String END_NAME = "도착";
	private static final double TOO_NEAR_METERS = 110.0;
	private static final double EARTH_RADIUS_METERS = 6371000;

	private final RestClient restClient;
	private final String appKey;

	public TmapRouteClient(@Value("${tmap.app-key}") String appKey) {
		this.appKey = appKey;
		this.restClient = RestClient.builder().build();
	}

	@Cacheable(cacheNames = "route-pedestrian", key = "#startX + ',' + #startY + ',' + #endX + ',' + #endY")
	public TmapRouteDto getPedestrianRoute(double startX, double startY, double endX, double endY) {
		if (distanceMeters(startY, startX, endY, endX) < TOO_NEAR_METERS) {
			return new TmapRouteDto(0, List.of(new RoutePointDto(startY, startX)));
		}

		JsonNode response;
		try {
			response = restClient.post()
					.uri(uriBuilder -> uriBuilder
							.scheme("https")
							.host("apis.openapi.sk.com")
							.path("/tmap/routes/pedestrian")
							.queryParam("version", "1")
							.build())
					.header("appKey", appKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of(
							"startX", String.valueOf(startX),
							"startY", String.valueOf(startY),
							"endX", String.valueOf(endX),
							"endY", String.valueOf(endY),
							"startName", START_NAME,
							"endName", END_NAME,
							"reqCoordType", "WGS84GEO",
							"resCoordType", "WGS84GEO"
					))
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientResponseException e) {
			throw new ExternalApiException("Tmap 요청 실패(status=" + e.getStatusCode().value() + ")");
		} catch (RestClientException e) {
			throw new ExternalApiException("Tmap 요청 실패: " + e.getMessage());
		}

		return parseRoute(response);
	}

	private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_METERS * c;
	}

	private TmapRouteDto parseRoute(JsonNode response) {
		validApi(response);
		
		int totalTimeSeconds = 0;
		List<RoutePointDto> points = new ArrayList<>();

		JsonNode features = response.path("features");
		for (JsonNode feature : features) {
			JsonNode properties = feature.path("properties");
			JsonNode geometry = feature.path("geometry");

			if (properties.has("totalTime")) {
				totalTimeSeconds = Math.max(totalTimeSeconds, properties.path("totalTime").asInt());
			}

			if ("LineString".equals(geometry.path("type").asString())) {
				for (JsonNode coord : geometry.path("coordinates")) {
					double lng = coord.get(0).asDouble();
					double lat = coord.get(1).asDouble();
					points.add(new RoutePointDto(lat, lng));
				}
			}
		}

		validPoints(points);

		int timeMinutes = (int) Math.ceil(totalTimeSeconds / 60.0);
		return new TmapRouteDto(timeMinutes, points);
	}
	
	private void validApi(JsonNode response) {
		if (response == null) {
			throw new ExternalApiException("Tmap 응답을 받지 못했습니다.");
		}
		
		JsonNode features = response.path("features");
		if (!features.isArray() || features.isEmpty()) {
			throw new NotFoundException("두 지점을 잇는 도보 경로를 찾지 못했습니다.");
		}
	}
	
	private void validPoints(List<RoutePointDto> points) {
		if (points.isEmpty()) {
			throw new NotFoundException("도보 경로 좌표를 받지 못했습니다.");
		}
	}
	
}

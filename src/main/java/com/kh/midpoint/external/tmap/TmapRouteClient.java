package com.kh.midpoint.external.tmap;

import com.kh.midpoint.common.exception.ExternalApiException;
import com.kh.midpoint.common.exception.NotFoundException;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TmapRouteClient {

	// 상수는 전부 application-constant.yml 에 있다. 여기에 기본값을 적지 않는 이유는
	// 출처를 한 곳으로 유지하기 위해서다(키가 빠지면 어떤 키인지 알려주며 기동이 실패한다).
	@Value("${route.start}")
	private String startName;

	@Value("${route.end}")
	private String endName;

	@Value("${route.near}")
	private double nearMeter;

	@Value("${route.radius}")
	private double earthRadiusMeter;

	private final RestClient restClient;
	private final String appKey;
	private final String routeUrl;

	// 타임아웃과 URL 은 생성자에서 RestClient 를 만들 때 필요하다. 필드 주입은 생성자 이후라
	// 늦으므로 생성자 파라미터로 받는다.
	public TmapRouteClient(@Value("${tmap.app-key}") String appKey,
			@Value("${route.url}") String routeUrl,
			@Value("${external.timeout.connect}") long connectTimeoutMillis,
			@Value("${external.timeout.read}") long readTimeoutMillis) {
		this.appKey = appKey;
		this.routeUrl = routeUrl;

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMillis));
		requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMillis));

		this.restClient = RestClient.builder()
				.requestFactory(requestFactory)
				.build();
	}

	@Cacheable(cacheNames = "route-pedestrian", key = "#startX + ',' + #startY + ',' + #endX + ',' + #endY")
	public TmapRouteDto getPedestrianRoute(double startX, double startY, double endX, double endY) {
		if (distanceMeters(startY, startX, endY, endX) < nearMeter) {
			return new TmapRouteDto(0, List.of(new RoutePointDto(startY, startX)));
		}

		JsonNode response;
		try {
			response = restClient.post()
								 .uri(routeUrl)
								 .header("appKey", appKey)
								 .contentType(MediaType.APPLICATION_JSON)
								 .body(Map.of(
										 	   "startX", String.valueOf(startX),
										 	   "startY", String.valueOf(startY),
										 	   "endX", String.valueOf(endX),
										 	   "endY", String.valueOf(endY),
										 	   "startName", startName,
										 	   "endName", endName,
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
		return earthRadiusMeter * c;
	}

	private TmapRouteDto parseRoute(JsonNode response) {
		validateApi(response);
		
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

		validatePoints(points);

		int timeMinutes = (int) Math.ceil(totalTimeSeconds / 60.0);
		return new TmapRouteDto(timeMinutes, points);
	}
	
	private void validateApi(JsonNode response) {
		if (response == null) {
			throw new ExternalApiException("Tmap 응답을 받지 못했습니다.");
		}
		
		JsonNode features = response.path("features");
		if (!features.isArray() || features.isEmpty()) {
			throw new NotFoundException("두 지점을 잇는 도보 경로를 찾지 못했습니다.");
		}
	}
	
	private void validatePoints(List<RoutePointDto> points) {
		if (points.isEmpty()) {
			throw new NotFoundException("도보 경로 좌표를 받지 못했습니다.");
		}
	}
	
}

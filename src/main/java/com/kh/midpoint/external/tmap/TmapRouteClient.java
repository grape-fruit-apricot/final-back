package com.kh.midpoint.external.tmap;

import com.kh.midpoint.common.exception.ExternalApiException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.route.model.dto.RoutePointDto;
import com.kh.midpoint.route.model.dto.RouteSegmentDto;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class TmapRouteClient {
	@Value("${route.segment-type.walking}")
	private String walking;

	// 상수는 전부 application-constant.yml 에 있다. 여기에 기본값을 적지 않는 이유는
	// 출처를 한 곳으로 유지하기 위해서다(키가 빠지면 어떤 키인지 알려주며 기동이 실패한다).
	@Value("${route.start}")
	private String startName;

	@Value("${route.end}")
	private String endName;

	@Value("${route.minimum-distance-meters}")
	private double minimumDistanceMeters;

	@Value("${route.earth-radius-meters}")
	private double earthRadiusMeters;

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
		if (calculateDistanceMeters(startY, startX, endY, endX) < minimumDistanceMeters) {
			List<RoutePointDto> points = List.of(
					new RoutePointDto(startY, startX),
					new RoutePointDto(endY, endX));
			List<RouteSegmentDto> segments = List.of(new RouteSegmentDto(
					0, walking, 0, null, List.of(), points));
			return new TmapRouteDto(0, points, segments);
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
										   "resCoordType", "WGS84GEO",
										   "sort", "index"
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

	private double calculateDistanceMeters(double startLat, double startLng,
			double endLat, double endLng) {
		double latDifference = Math.toRadians(endLat - startLat);
		double lngDifference = Math.toRadians(endLng - startLng);
		double calculation = Math.sin(latDifference / 2) * Math.sin(latDifference / 2)
				+ Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat))
				* Math.sin(lngDifference / 2) * Math.sin(lngDifference / 2);
		double centralAngle = 2 * Math.atan2(Math.sqrt(calculation), Math.sqrt(1 - calculation));
		return earthRadiusMeters * centralAngle;
	}

	private TmapRouteDto parseRoute(JsonNode response) {
		validateApi(response);
		
		int totalTimeSeconds = 0;
		List<RoutePointDto> points = new ArrayList<>();
		List<RouteSegmentDto> segments = new ArrayList<>();
		List<JsonNode> lineFeatures = new ArrayList<>();

		JsonNode features = response.path("features");
		for (JsonNode feature : features) {
			JsonNode properties = feature.path("properties");

			if (properties.has("totalTime")) {
				totalTimeSeconds = Math.max(totalTimeSeconds, properties.path("totalTime").asInt());
			}

			if ("LineString".equals(feature.path("geometry").path("type").asString())) {
				lineFeatures.add(feature);
			}
		}

		// 카카오 대중교통의 steps처럼 Tmap도 properties.index가 실제 안내 순서를
		// 나타낸다. 응답 배열 순서에 의존하면 구간이 뒤섞여 지도에서 왕복하는 선이 생길 수 있다.
		lineFeatures.sort(Comparator.comparingInt(
				feature -> feature.path("properties").path("index").asInt(Integer.MAX_VALUE)));

		for (JsonNode feature : lineFeatures) {
			JsonNode properties = feature.path("properties");
			List<RoutePointDto> segmentPoints = new ArrayList<>();
			for (JsonNode coord : feature.path("geometry").path("coordinates")) {
				if (coord.isArray() && coord.size() >= 2) {
					double lng = coord.get(0).asDouble();
					double lat = coord.get(1).asDouble();
					segmentPoints.add(new RoutePointDto(lat, lng));
				}
			}
			if (!segmentPoints.isEmpty()) {
				int segmentTimeMinutes = (int) Math.ceil(properties.path("time").asInt(0) / 60.0);
				String guidance = properties.path("description").asString("");
				segments.add(new RouteSegmentDto(
						segments.size(), walking, segmentTimeMinutes,
						guidance.isBlank() ? null : guidance, List.of(), segmentPoints));
				points.addAll(segmentPoints);
			}
		}

		validatePoints(points);

		int timeMinutes = (int) Math.ceil(totalTimeSeconds / 60.0);
		return new TmapRouteDto(timeMinutes, points, segments);
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

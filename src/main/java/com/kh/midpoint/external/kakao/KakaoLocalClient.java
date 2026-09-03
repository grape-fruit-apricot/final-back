package com.kh.midpoint.external.kakao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.kh.midpoint.common.exception.ExternalApiException;
import com.kh.midpoint.restaurant.model.dto.KakaoRestaurantResponseDto;

import tools.jackson.databind.JsonNode;

@Component
public class KakaoLocalClient {

	@Value("${search.category.subway.code}")
	private String subwayCode;

	@Value("${search.category.subway.radius}")
	private int subwayRadius;

	@Value("${search.category.restaurant.code}")
	private String restaurantCode;

	@Value("${search.category.restaurant.radius}")
	private int restaurantRadius;

	@Value("${search.category.restaurant.result-size}")
	private int restaurantResultSize;

	private final RestClient categoryClient;

	public KakaoLocalClient(@Value("${kakao.rest-api-key}") String kakaoRestApiKey,
							@Value("${search.category.url}") String categoryUrl) {
		this.categoryClient = RestClient.builder()
				.baseUrl(categoryUrl)
				.defaultHeader("Authorization", "KakaoAK " + kakaoRestApiKey)
				.build();
	}

	@Cacheable(cacheNames = "restaurants-nearby", key = "#lat + ',' + #lng")
	public List<KakaoRestaurantResponseDto> findNearbyRestaurantList(Double lat, Double lng) {
		JsonNode response = searchCategory(restaurantCode, lng, lat, restaurantRadius, "accuracy", restaurantResultSize);

		return toRestaurantResponseList(response);
	}

	// 두 검색이 같은 카테고리 API 를 같은 파라미터 형태로 호출하므로 요청과 예외 변환을 한 곳에 모은다.
	// 외부 API 실패를 그대로 흘려보내면 500 이 나가므로 ExternalApiException(502) 으로 바꾼다.
	private JsonNode searchCategory(String categoryCode, double x, double y, int radius, String sort, int size) {
		try {
			return categoryClient.get()
					.uri(uriBuilder -> uriBuilder
							.queryParam("category_group_code", categoryCode)
							.queryParam("x", x)
							.queryParam("y", y)
							.queryParam("radius", radius)
							.queryParam("sort", sort)
							.queryParam("size", size)
							.build())
					.retrieve()
					.body(JsonNode.class);

		} catch (RestClientResponseException e) {
			throw new ExternalApiException("카카오 요청 실패(status=" + e.getStatusCode().value() + ")");
		} catch (RestClientException e) {
			throw new ExternalApiException("카카오 요청 실패: " + e.getMessage());
		}
	}

	private List<KakaoRestaurantResponseDto> toRestaurantResponseList(JsonNode response) {
		List<KakaoRestaurantResponseDto> restaurants = new ArrayList<>();
		if (response == null) {
			return restaurants;
		}

		for (JsonNode document : response.path("documents")) {
			restaurants.add(new KakaoRestaurantResponseDto(
					document.path("id").asString(),
					document.path("place_name").asString(),
					document.path("category_name").asString(),
					document.path("address_name").asString(),
					document.path("road_address_name").asString(),
					document.path("phone").asString(),
					document.path("place_url").asString(),
					document.path("y").asDouble(),
					document.path("x").asDouble(),
					document.path("distance").asInt(0)
			));
		}

		return restaurants;
	}
	
	@Cacheable(cacheNames = "stations-nearby", key = "#x + ',' + #y + ',' + #count")
	public List<NearbyStationDto> findNearbySubwayStations(double x, double y, int count) {
		JsonNode response = searchCategory(subwayCode, x, y, subwayRadius, "distance", count);

		List<NearbyStationDto> stations = new ArrayList<>();
		if (response == null) {
			return stations;
		}
		for (JsonNode doc : response.path("documents")) {
			stations.add(new NearbyStationDto(
					doc.path("place_name").asString(),
					doc.path("y").asDouble(),
					doc.path("x").asDouble()
			));
		}
		return stations;
	}
}

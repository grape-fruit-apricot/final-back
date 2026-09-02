package com.kh.midpoint.external.kakao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.kh.midpoint.restaurant.model.dto.KakaoRestaurantResponseDto;

import tools.jackson.databind.JsonNode;

@Component
public class KakaoLocalClient {

	@Value("${search.category.subway.code}")
	private String subwayCode;

	@Value("${search.category.subway.radius}")
	private String subwayRadius;

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
		JsonNode response = categoryClient.get()
				.uri(uriBuilder -> uriBuilder
						.queryParam("category_group_code", restaurantCode)
						.queryParam("x", lng)
						.queryParam("y", lat)
						.queryParam("radius", restaurantRadius)
						.queryParam("sort", "accuracy")
						.queryParam("size", restaurantResultSize)
						.build())
				.retrieve()
				.body(JsonNode.class);

		return toRestaurantResponseList(response);
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
		JsonNode response = categoryClient.get()
				.uri(uriBuilder -> uriBuilder
						.queryParam("category_group_code", subwayCode)
						.queryParam("x", x)
						.queryParam("y", y)
						.queryParam("radius", subwayRadius)
						.queryParam("sort", "distance")
						.queryParam("size", count)
						.build())
				.retrieve()
				.body(JsonNode.class);

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

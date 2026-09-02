package com.kh.midpoint.external.kakao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

@Component
public class KakaoLocalClient {

	private static final String CATEGORY_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/category.json";
	private static final String SUBWAY_CATEGORY_GROUP_CODE = "SW8";
	private static final int SUBWAY_SEARCH_RADIUS_METERS = 10000;

	private final RestClient categoryClient;

	public KakaoLocalClient(@Value("${kakao.rest-api-key}") String kakaoRestApiKey) {
		this.categoryClient = RestClient.builder()
				.baseUrl(CATEGORY_SEARCH_URL)
				.defaultHeader("Authorization", "KakaoAK " + kakaoRestApiKey)
				.build();
	}
	
	@Cacheable(cacheNames = "stations-nearby", key = "#x + ',' + #y + ',' + #count")
	public List<NearbyStationDto> findNearbySubwayStations(double x, double y, int count) {
		JsonNode response = categoryClient.get()
				.uri(uriBuilder -> uriBuilder
						.queryParam("category_group_code", SUBWAY_CATEGORY_GROUP_CODE)
						.queryParam("x", x)
						.queryParam("y", y)
						.queryParam("radius", SUBWAY_SEARCH_RADIUS_METERS)
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

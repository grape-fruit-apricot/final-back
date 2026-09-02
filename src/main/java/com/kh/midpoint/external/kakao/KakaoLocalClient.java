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

	private final RestClient categoryClient;
	
	@Value("${search.category.url}")
	private String categoryUrl;

	@Value("${search.category.subway.code}")
	private String subwayCode;

	@Value("${search.category.subway.radius}")
	private String subwayRadius;
	
	public KakaoLocalClient(@Value("${kakao.rest-api-key}") String kakaoRestApiKey) {
		this.categoryClient = RestClient.builder()
				.baseUrl(categoryUrl)
				.defaultHeader("Authorization", "KakaoAK " + kakaoRestApiKey)
				.build();
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

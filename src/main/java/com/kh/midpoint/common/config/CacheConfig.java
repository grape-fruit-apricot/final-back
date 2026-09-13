package com.kh.midpoint.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

// 외부 API 클라이언트의 @Cacheable 은 @EnableCaching 과 CacheManager 빈이 둘 다 있어야 동작한다.
// 이 설정이 없으면 애너테이션만 붙어 있고 매 호출이 그대로 네트워크로 나간다.
//
// 이전에는 ConcurrentMapCacheManager 를 썼는데 TTL 도 최대 크기도 없었다.
// 캐시 키가 좌표라서 방이 만료되어 지워져도 항목이 남았고, 비우는 경로가 아예 없어
// 오래 띄워둘수록 계속 쌓였다. 특히 route-pedestrian 은 값이 도보 경로 좌표 전량이라
// 항목 하나가 수십 KB 다.
//
// 캐시 세 종류는 크기가 크게 달라 상한을 따로 준다. 값 크기와 증가 속도는
// application-constant.yml 의 주석에 적어두었다.
@EnableCaching
@Configuration
public class CacheConfig {

	@Value("${cache.restaurants-nearby.maximum-size}")
	private long restaurantsNearbyMaximumSize;

	@Value("${cache.restaurants-nearby.ttl-minutes}")
	private long restaurantsNearbyTtlMinutes;

	@Value("${cache.stations-nearby.maximum-size}")
	private long stationsNearbyMaximumSize;

	@Value("${cache.stations-nearby.ttl-minutes}")
	private long stationsNearbyTtlMinutes;

	@Value("${cache.route-pedestrian.maximum-size}")
	private long routePedestrianMaximumSize;

	@Value("${cache.route-pedestrian.ttl-minutes}")
	private long routePedestrianTtlMinutes;

	@Value("${cache.route-transit.maximum-size}")
	private long routeTransitMaximumSize;

	@Value("${cache.route-transit.ttl-minutes}")
	private long routeTransitTtlMinutes;

	@Bean
	public CacheManager cacheManager() {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(List.of(
				buildCache("restaurants-nearby", restaurantsNearbyMaximumSize, restaurantsNearbyTtlMinutes),
				buildCache("stations-nearby", stationsNearbyMaximumSize, stationsNearbyTtlMinutes),
				buildCache("route-pedestrian", routePedestrianMaximumSize, routePedestrianTtlMinutes),
				buildCache("route-transit", routeTransitMaximumSize, routeTransitTtlMinutes)
		));
		return cacheManager;
	}

	// 최대 크기를 넘으면 오래 쓰이지 않은 항목부터 밀려나고, 쓴 지 TTL 이 지난 항목은 만료된다.
	// 캐시가 비어도 다음 호출이 외부 API 를 한 번 더 탈 뿐이라 정확성에는 영향이 없다.
	private CaffeineCache buildCache(String name, long maximumSize, long ttlMinutes) {
		return new CaffeineCache(name, Caffeine.newBuilder()
				.maximumSize(maximumSize)
				.expireAfterWrite(Duration.ofMinutes(ttlMinutes))
				.build());
	}

}

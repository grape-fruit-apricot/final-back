package com.kh.midpoint.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 외부 API 클라이언트의 @Cacheable 은 @EnableCaching 과 CacheManager 빈이 둘 다 있어야 동작한다.
// 이 설정이 없으면 애너테이션만 붙어 있고 매 호출이 그대로 네트워크로 나간다.
//
// spring-boot-starter-cache 는 의존성에 없어서(새 라이브러리 추가는 팀 승인이 필요하다)
// spring-context 에 이미 들어 있는 ConcurrentMapCacheManager 를 직접 등록한다.
// 주의: 이 구현에는 TTL 과 최대 크기가 없다. 좌표별로 항목이 계속 쌓이므로 오래 띄워두는
// 운영 환경에서는 만료가 있는 캐시로 바꿔야 한다.
@EnableCaching
@Configuration
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager("restaurants-nearby", "stations-nearby", "route-pedestrian");
	}

}

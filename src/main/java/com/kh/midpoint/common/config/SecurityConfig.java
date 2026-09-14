package com.kh.midpoint.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	// 허용할 프론트 주소 목록. 값은 application-constant.yml 에서 쉼표로 구분해 관리한다.
	// 쉼표 문자열을 List 로 바꿔주는 것은 Spring 의 기본 변환이라 별도 설정이 필요 없다.
	@Value("${cors.allowed-origins}")
	private List<String> allowedOrigins;

	@Value("${management.server.port:-1}")
	private int managementPort;

	@Value("${server.port:8080}")
	private int serverPort;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.authorizeHttpRequests(auth -> auth
				// 별도 관리 포트의 두 GET 경로만 허용한다. 전달 헤더는 신뢰하지 않는다.
				.requestMatchers(request -> managementPort > 0
					&& managementPort != serverPort
					&& request.getLocalPort() == managementPort
					&& "GET".equals(request.getMethod())
					&& ("/actuator/health".equals(request.getServletPath())
						|| "/actuator/prometheus".equals(request.getServletPath())))
				.permitAll()
				.requestMatchers("/actuator", "/actuator/**").denyAll()
				.anyRequest().permitAll());

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}

package com.kh.midpoint.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 로그인 없이 동작하는 서비스라 현재는 모든 요청을 허용한다.
 *
 * spring-boot-starter-security 가 의존성에 들어 있으면 이 설정이 없을 때
 * 기본 보안 필터가 모든 요청을 401 로 막는다. /ws 핸드셰이크도 마찬가지다.
 *
 * TODO: 방 참가자 검증 정책이 정해지면 경로별로 다시 잡을 것
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}

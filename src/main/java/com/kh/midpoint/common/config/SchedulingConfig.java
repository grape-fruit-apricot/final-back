package com.kh.midpoint.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled 는 이 설정이 있어야 실제로 돈다. 없으면 애너테이션만 붙어 있고 아무 일도 일어나지 않는다.
//
// 이 프로젝트에서 주기 실행은 만료된 방 정리(RoomCleanupScheduler)가 처음이다.
// WebSocketConfig 에도 스케줄러가 있지만 그것은 STOMP 하트비트 전용이라 이것과 무관하다.
//
// 스케줄 작업이 늘어나면 기본 스레드 1개를 서로 기다리게 되므로, 그때 풀 크기를 정해야 한다.
// 지금은 작업이 하나뿐이라 기본값을 그대로 쓴다.
@EnableScheduling
@Configuration
public class SchedulingConfig {
}

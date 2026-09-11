package com.kh.midpoint.room.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import com.kh.midpoint.room.model.service.RoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 만료된 방을 주기적으로 지운다.
//
// ROOM.EXPIRES_AT 은 방을 만들 때 생성 시각 + 3시간으로 들어가지만, 지금까지 그 값을
// 읽는 코드가 어디에도 없어서 방이 계속 쌓이기만 했다. 과제 요건인 "외부 데이터의 관리"는
// 저장뿐 아니라 수명이 끝난 데이터를 치우는 것까지다.
//
// 방을 지우면 참가자·식당·선택·투표·게임·경로·추적 이력이 FK 의 ON DELETE CASCADE 로
// 함께 사라진다(실 DB 에서 3단계 아래인 LOCATION_LOG, PARTICIPANT_ROUTE_POINT 까지 닿는 것을
// 확인했다). 그래서 이 클래스는 방만 지운다.
//
// cleanup.enabled 가 true 인 서버에서만 이 빈이 만들어진다. develop 과 main 이 같은 Oracle 을
// 보기 때문에, 둘 다 켜면 같은 방을 동시에 지우려 하면서 잠금 대기가 생긴다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cleanup.enabled", havingValue = "true")
public class RoomCleanupScheduler {

	@Value("${cleanup.batch-size}")
	private int batchSize;

	@Value("${cleanup.interval-ms}")
	private long intervalMs;

	private final RoomService roomService;

	// 이 빈이 만들어졌다는 것은 이 서버가 방을 지우는 쪽이라는 뜻이다.
	// 두 서버 중 한쪽만 켜는 구조라, 나중에 "왜 방이 사라졌는지" 를 찾을 때
	// 어느 서버를 봐야 하는지 기동 로그에서 바로 알 수 있어야 한다.
	@PostConstruct
	public void logEnabled() {
		log.info("만료된 방 정리가 켜져 있습니다. {}ms 마다 최대 {}개씩 지웁니다.", intervalMs, batchSize);
	}

	// fixedDelay 로 둔다. fixedRate 는 앞 실행이 끝나기 전에 다음 실행을 시작하므로,
	// 지울 방이 많아 한 번이 오래 걸리면 실행이 겹치면서 같은 방을 두고 서로 기다리게 된다.
	@Scheduled(fixedDelayString = "${cleanup.interval-ms}", initialDelayString = "${cleanup.interval-ms}")
	public void deleteExpiredRoomList() {
		try {
			int deletedRooms = roomService.deleteExpiredRoomList(batchSize);
			// 지운 것이 없으면 로그를 남기지 않는다. 대부분의 주기는 0 건이라
			// 남기면 다른 로그가 묻힌다.
			if (deletedRooms > 0) {
				log.info("만료된 방 {}개를 정리했습니다.", deletedRooms);
			}
		} catch (Exception e) {
			// 여기서 막지 않으면 예외가 스케줄러 밖으로 나가고, 그 뒤로 이 작업이 다시 잡히지 않는다.
			// 다음 주기에 그대로 다시 시도하면 되는 일이라 기록만 남기고 넘어간다.
			log.warn("만료된 방 정리에 실패했습니다. 다음 주기에 다시 시도합니다 - {}", e.toString());
		}
	}

}

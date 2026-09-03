package com.kh.midpoint.point.controller;


import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.common.response.SocketErrorResponseDto;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.point.model.service.MidPointService;
import com.kh.midpoint.restaurant.model.service.RestaurantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MidPointController {

	private final MidPointService midPointService;
	private final RestaurantService restaurantService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/midpoint/find")
	public void findMidpoint(SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 중간지점 찾기 요청을 무시합니다.");
			return;
		}

		NearbyStationDto midpoint = midPointService.findMidpoint(session.roomUuid(), session.participantId());
		insertNearbyRestaurantList(session.roomUuid(), midpoint);

		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/midpoint", midpoint);
	}

	// 중간지점 트랜잭션이 끝난 뒤에 따로 호출한다. 같은 트랜잭션에서 돌리면 카카오 식당 조회가
	// 실패했을 때 확정된 중간지점까지 롤백된다. 식당 목록은 없어도 중간지점은 살아 있어야 하므로
	// 실패를 삼키고 로그만 남긴다. 브로드캐스트보다 먼저 저장해야 참가자들이 빈 목록을 보지 않는다.
	private void insertNearbyRestaurantList(String roomUuid, NearbyStationDto midpoint) {
		try {
			restaurantService.insertNearbyRestaurantList(roomUuid, midpoint.getLat(), midpoint.getLng());
		} catch (RuntimeException e) {
			log.warn("주변 식당 저장 실패 - roomUuid={}, {}", roomUuid, e.toString());
		}
	}

	@MessageExceptionHandler
	public void handleFindMidpointException(Exception e, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor);
		if (session == null) {
			return;
		}

		log.warn("중간지점 찾기 실패 - {}", e.toString());

		String message = e.getMessage() == null ? "중간지점을 찾지 못했습니다." : e.getMessage();
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/midpoint/error",
				new SocketErrorResponseDto(message));
	}


}

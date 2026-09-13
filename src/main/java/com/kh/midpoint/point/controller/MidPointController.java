package com.kh.midpoint.point.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.point.model.dto.MidPointRequestDto;
import com.kh.midpoint.point.model.service.MidPointService;
import com.kh.midpoint.restaurant.model.service.RestaurantService;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.selection.model.service.SelectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MidPointController {

	@Value("${chat.session-attribute-key}")
	private String sessionAttributeKey;

	private final MidPointService midPointService;
	private final RestaurantService restaurantService;
	private final SimpMessagingTemplate messagingTemplate;
	private final ParticipantService participantService;
	private final SelectionService selectionService;

	@MessageMapping("/midpoint/reset")
	public void resetMidpoint(@Valid @Payload MidPointRequestDto requestDto,
			SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor, sessionAttributeKey);
		if (session == null) {
			return;
		}
		String topic = "/topic/room/" + session.roomUuid();
		NearbyStationDto midpoint = midPointService.resetMidpoint(
				session.roomUuid(), session.participantId(), requestDto.getTravelMode());
		// 커밋 이후에만 알린다. 재설정 이벤트의 좌표는 재계산이 완료된 새 좌표다.
		messagingTemplate.convertAndSend(topic + "/midpoint/reset", midpoint);
		messagingTemplate.convertAndSend(topic + "/midpoint", midpoint);
		messagingTemplate.convertAndSend(topic + "/restaurants", restaurantService.findRestaurantList(session.roomUuid()));
		messagingTemplate.convertAndSend(topic + "/selections", selectionService.findSelectionList(session.roomUuid()));
		messagingTemplate.convertAndSend(topic + "/participants/ready", participantService.findParticipantList(session.roomUuid()));
	}

	@MessageMapping("/midpoint/find")
	public void findMidpoint(@Valid @Payload MidPointRequestDto requestDto,
			SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor, sessionAttributeKey);
		if (session == null) {
			log.warn("검증되지 않은 연결이라 중간지점 찾기 요청을 무시합니다.");
			return;
		}

		NearbyStationDto midpoint = midPointService.findMidpoint(
				session.roomUuid(), session.participantId(), requestDto.getTravelMode());

		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + "/midpoint", midpoint);
	}

}

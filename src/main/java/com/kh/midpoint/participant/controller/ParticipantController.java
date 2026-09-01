package com.kh.midpoint.participant.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.participant.model.dto.JoinRoomRequest;
import com.kh.midpoint.participant.model.service.ParticipantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {
	
	private final ParticipantService participantService;
	
	@PostMapping("/{roomUuid}")
	public ResponseEntity<ApiResponse<Map<String, String>>> join(@PathVariable("roomUuid") String roomUuid, @RequestBody JoinRoomRequest request) {
		log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ {}",roomUuid);
		Long participantId = participantService.join(roomUuid, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("참여자가 등록되었습니다.", Map.of("id", String.valueOf(participantId))));
	}

}

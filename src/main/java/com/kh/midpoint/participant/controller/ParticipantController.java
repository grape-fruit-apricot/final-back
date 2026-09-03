package com.kh.midpoint.participant.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.participant.model.dto.JoinRoomRequest;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms/{roomUuid}/participants")
@RequiredArgsConstructor
public class ParticipantController {

	private final ParticipantService participantService;
	private final SimpMessagingTemplate messagingTemplate;

	@PostMapping
	public ResponseEntity<ApiResponse<ParticipantResponseDto>> join(@PathVariable("roomUuid") String roomUuid, @RequestBody JoinRoomRequest request) {
		ParticipantResponseDto responseDto = participantService.join(roomUuid, request);
		messagingTemplate.convertAndSend("/topic/room/" + roomUuid + "/participants", responseDto);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("참여자가 등록되었습니다.", responseDto));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ParticipantResponseDto>>> findAllParticipantList(@PathVariable("roomUuid") String roomUuid) {
		List<ParticipantResponseDto> responseDto = participantService.findAllParticipants(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("참가자 목록 조회에 성공했습니다.", responseDto));
	}

	@PatchMapping("/{participantId}/ready")
	public ResponseEntity<ApiResponse<Void>> updateReady(@PathVariable("roomUuid") String roomUuid, @PathVariable("participantId") Long participantId) {
		participantService.updateReady(roomUuid, participantId);

		// 준비 현황은 방 전체가 함께 보는 정보라 갱신된 참가자 목록을 통째로 브로드캐스트한다.
		// 입장 토픽은 새 참가자 1명만 보내므로 목록 전체를 보내는 별도 토픽을 쓴다.
		messagingTemplate.convertAndSend("/topic/room/" + roomUuid + "/participants/ready",
				participantService.findAllParticipants(roomUuid));

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("준비 상태가 변경되었습니다.", null));
	}

	@DeleteMapping("/{participantId}")
	public ResponseEntity<Void> leave(@PathVariable("roomUuid") String roomUuid, @PathVariable("participantId") Long participantId) {
		participantService.deleteParticipant(roomUuid, participantId);
		return ResponseEntity.noContent().build();
	}

}

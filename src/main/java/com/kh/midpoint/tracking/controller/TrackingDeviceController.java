package com.kh.midpoint.tracking.controller;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.tracking.model.dto.LocationRequestDto;
import com.kh.midpoint.tracking.model.dto.LocationResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingAssignmentResponseDto;
import com.kh.midpoint.tracking.model.service.TrackingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 외부 디바이스(라즈베리파이)가 쓰는 자리. 다른 API 와 달리 방 단위 경로가 아닌 이유는,
// 디바이스가 어느 방을 맡을지 미리 모르기 때문이다. 자기 키로 배정을 받아온 뒤에야 방을 알게 된다.
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingDeviceController {

	private final TrackingService trackingService;
	private final SimpMessagingTemplate messagingTemplate;

	@GetMapping("/devices/{deviceKey}/sessions")
	public ResponseEntity<ApiResponse<List<TrackingAssignmentResponseDto>>> findTrackingAssignmentList(
			@PathVariable("deviceKey") String deviceKey) {
		List<TrackingAssignmentResponseDto> responseDto =
				trackingService.findTrackingAssignmentList(deviceKey);
		return ResponseEntity.ok(ApiResponse.ok("추적 배정 조회에 성공했습니다.", responseDto));
	}

	@PostMapping("/locations")
	public ResponseEntity<ApiResponse<List<LocationResponseDto>>> insertLocationLogList(
			@Valid @RequestBody LocationRequestDto requestDto) {
		List<LocationResponseDto> responseDto = trackingService.insertLocationLogList(requestDto);

		// 좌표가 들어올 때마다 방 전체가 같은 화면을 봐야 한다. 디바이스 1대가 여러 방을 맡을 수도
		// 있으므로 방마다 한 번씩만 보낸다(LinkedHashSet 이 중복을 지우고 순서를 지킨다).
		Set<String> roomUuids = new LinkedHashSet<>(responseDto.stream()
				.map(LocationResponseDto::getRoomUuid)
				.toList());
		for (String roomUuid : roomUuids) {
			messagingTemplate.convertAndSend("/topic/room/" + roomUuid + "/tracking",
					trackingService.findTracking(roomUuid));
		}

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("좌표가 기록되었습니다.", responseDto));
	}

}

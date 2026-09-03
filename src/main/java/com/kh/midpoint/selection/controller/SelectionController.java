package com.kh.midpoint.selection.controller;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.selection.model.dto.SelectionRequestDto;
import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.service.SelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomUuid}")
@RequiredArgsConstructor
public class SelectionController {

	private final SelectionService selectionService;
	private final SimpMessagingTemplate messagingTemplate;

	@PostMapping("/participants/{participantId}/selection")
	public ResponseEntity<ApiResponse<SelectionResponseDto>> insertSelection(
			@PathVariable String roomUuid,
			@PathVariable Long participantId,
			@Valid @RequestBody SelectionRequestDto requestDto) {
		SelectionResponseDto responseDto = selectionService.insertSelection(roomUuid, participantId, requestDto);

		// 선택은 방 전체가 함께 보는 정보라, 갱신된 현황을 통째로 브로드캐스트한다.
		messagingTemplate.convertAndSend("/topic/room/" + roomUuid + "/selections",
				selectionService.findSelectionList(roomUuid));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("식당이 선택되었습니다.", responseDto));
	}

	@GetMapping("/selections")
	public ResponseEntity<ApiResponse<List<SelectionResponseDto>>> findSelectionList(@PathVariable String roomUuid) {
		List<SelectionResponseDto> responseDto = selectionService.findSelectionList(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("선택 현황 조회에 성공했습니다.", responseDto));
	}

}

package com.kh.midpoint.selection.controller;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.selection.model.dto.SelectionRequestDto;
import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.service.SelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomUuid}/participants/{participantId}/selection")
@RequiredArgsConstructor
public class SelectionController {

	private final SelectionService selectionService;

	@PostMapping
	public ResponseEntity<ApiResponse<SelectionResponseDto>> selectRestaurant(
			@PathVariable String roomUuid,
			@PathVariable Long participantId,
			@Valid @RequestBody SelectionRequestDto requestDto) {
		SelectionResponseDto responseDto = selectionService.selectRestaurant(roomUuid, participantId, requestDto);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("식당이 선택되었습니다.", responseDto));
	}

}

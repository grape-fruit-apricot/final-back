package com.kh.midpoint.room.controller;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.room.model.dto.RoomCreateRequestDto;
import com.kh.midpoint.room.model.dto.RoomCreateResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

	private final RoomService roomService;

	@PostMapping
	public ResponseEntity<ApiResponse<RoomCreateResponseDto>> insertRoom(@Valid @RequestBody RoomCreateRequestDto requestDto) {
		RoomCreateResponseDto responseDto = roomService.insertRoom(requestDto);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("방이 생성되었습니다.", responseDto));
	}

}

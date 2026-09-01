package com.kh.midpoint.room.controller;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.room.model.dto.RoomCreateRequestDto;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	public ResponseEntity<ApiResponse<RoomResponseDto>> insertRoom(@Valid @RequestBody RoomCreateRequestDto requestDto) {
		RoomResponseDto responseDto = roomService.insertRoom(requestDto);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("방이 생성되었습니다.", responseDto));
	}

	@GetMapping("/{roomUuid}")
	public ResponseEntity<ApiResponse<RoomResponseDto>> findRoom(@PathVariable String roomUuid) {
		RoomResponseDto responseDto = roomService.findRoom(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("방 조회에 성공했습니다.", responseDto));
	}

}
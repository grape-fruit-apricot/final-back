package com.kh.midpoint.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.chat.model.dto.ChatMessageResponseDto;
import com.kh.midpoint.chat.model.service.ChatService;
import com.kh.midpoint.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms/{roomUuid}/messages")
@RequiredArgsConstructor
public class ChatRestController {

	private final ChatService chatService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<ChatMessageResponseDto>>> findMessageList(
			@PathVariable("roomUuid") String roomUuid,
			@RequestParam(value = "afterMessageId", required = false) Long afterMessageId) {
		List<ChatMessageResponseDto> messages = chatService.findMessageList(roomUuid, afterMessageId);
		return ResponseEntity.ok(ApiResponse.ok("대화 내역을 조회했습니다.", messages));
	}

}

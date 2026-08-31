package com.kh.midpoint.chat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.chat.model.dto.ChatMessageResponse;
import com.kh.midpoint.chat.model.service.ChatService;

import lombok.RequiredArgsConstructor;

/**
 * 방 입장 시 이전 대화 내역을 불러오는 REST API.
 * WebConfig 의 CORS 매핑(/api/**)에 맞춰 경로를 /api 로 시작한다.
 *
 * TODO: 방 생성/참가 API 는 방 담당자가 추가 (/api/rooms)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping("/rooms/{roomUuid}/messages")
    public List<ChatMessageResponse> getMessages(@PathVariable("roomUuid") String roomUuid) {
        return chatService.getMessages(roomUuid);
    }
}

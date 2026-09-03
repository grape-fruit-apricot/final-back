package com.kh.midpoint.game.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.game.model.dto.GameStateResponseDto;
import com.kh.midpoint.game.model.service.GameService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms/{roomUuid}/games")
@RequiredArgsConstructor
public class GameController {
	private final GameService gameService;
	
	@GetMapping
    public ResponseEntity<ApiResponse<GameStateResponseDto>> findGameState(
            @PathVariable String roomUuid,
            @RequestParam Long participantId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "게임 상태 조회에 성공했습니다.",
                gameService.findGameState(roomUuid, participantId)));
    }
}

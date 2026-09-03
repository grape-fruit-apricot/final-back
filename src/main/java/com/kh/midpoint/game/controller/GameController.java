package com.kh.midpoint.game.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.game.model.dto.GameStateResponseDto;
import com.kh.midpoint.game.model.dto.GameStateUpdateRequestDto;
import com.kh.midpoint.game.model.service.GameService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms/{roomUuid}/games")
@RequiredArgsConstructor
public class GameController {
	private final GameService gameService;
	
	@PatchMapping("/state")
    public ResponseEntity<ApiResponse<GameStateResponseDto>> updateGameState(
            @PathVariable String roomUuid,
            @Valid @RequestBody GameStateUpdateRequestDto request) {
        GameStateResponseDto data = gameService.updateGameState(
                roomUuid,
                request.getParticipantId(),
                request.getVersion(),
                request.getState());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("게임 상태 변경에 성공했습니다.", data));
    }
}

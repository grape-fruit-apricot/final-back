package com.kh.midpoint.game.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.game.model.dto.GameDto.*;
import com.kh.midpoint.game.model.service.GameService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@CrossOrigin("*")
public class GameController {
	private final GameService gameService;
	
	@PostMapping("/{gameId}/join")
    public ResponseEntity<ApiResponse<GameResponse>> insertPlayer(
            @PathVariable("gameId") Long gameId, @Valid @RequestBody JoinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("게임 참가에 성공했습니다.",
                        gameService.insertPlayer(gameId, request.getPlayerName())));
    }
	
}

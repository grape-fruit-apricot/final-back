package com.kh.midpoint.game.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.game.model.dto.GameResponse;
import com.kh.midpoint.game.model.service.GameService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@CrossOrigin("*")
public class GameController {
	private final GameService gameService;
	
	@GetMapping("/{gameId}")
    public ResponseEntity<ApiResponse<GameResponse>> findGame(
            @PathVariable Long gameId, @RequestParam Long participantId) {
        return ResponseEntity.ok(ApiResponse.ok("게임 조회에 성공했습니다.",
                gameService.findGame(gameId, participantId)));
    }
}

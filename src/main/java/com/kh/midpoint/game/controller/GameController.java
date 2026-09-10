package com.kh.midpoint.game.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.game.model.dto.GameStatusDto;
import com.kh.midpoint.game.model.service.GameService;

import lombok.RequiredArgsConstructor;

// 새로고침이나 재접속에서 게임 현황을 복원하기 위한 조회 전용 경로.
// 시작·선택·만료·나가기는 소켓으로만 받는다(행위자를 세션에서 확인해야 하므로).
@RestController
@RequestMapping("/api/rooms/{roomUuid}/games")
@RequiredArgsConstructor
public class GameController {

	private final GameService gameService;

	@GetMapping
	public ResponseEntity<ApiResponse<GameStatusDto>> findGameStatus(@PathVariable("roomUuid") String roomUuid) {
		GameStatusDto responseDto = gameService.findGameStatus(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("게임 현황 조회에 성공했습니다.", responseDto));
	}

}

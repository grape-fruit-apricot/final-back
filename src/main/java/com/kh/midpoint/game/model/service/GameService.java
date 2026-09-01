package com.kh.midpoint.game.model.service;

import org.springframework.stereotype.Service;

import com.kh.midpoint.game.model.dto.GameResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
	private final GamePlayerService playerService;
	
	public GameResponse insertPlayer(Long gameId, String playerName) {
        return playerService.insertPlayer(gameId, playerName);
    }
}

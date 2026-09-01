package com.kh.midpoint.game.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.game.model.dto.GameResponse;
import com.kh.midpoint.game.model.vo.Game;
import com.kh.midpoint.game.model.vo.GamePlayer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GamePlayerService {

	@Transactional
    GameResponse insertPlayer(Long gameId, String playerName) {
        Game game = validateJoinableGame(gameId);
        String name = validatePlayerName(gameId, playerName);
        int order = findAvailableOrder(gameId);
        GamePlayer player = savePlayer(gameId, name, order);
        return synchronizeAndRespond(game, player);
    }

	private GameResponse synchronizeAndRespond(Game game, GamePlayer player) {
		// TODO Auto-generated method stub
		return null;
	}

	private GamePlayer savePlayer(Long gameId, String name, int order) {
		// TODO Auto-generated method stub
		return null;
	}

	private int findAvailableOrder(Long gameId) {
		// TODO Auto-generated method stub
		return 0;
	}

	private String validatePlayerName(Long gameId, String playerName) {
		// TODO Auto-generated method stub
		return null;
	}

	private Game validateJoinableGame(Long gameId) {
		// TODO Auto-generated method stub
		return null;
	}
}

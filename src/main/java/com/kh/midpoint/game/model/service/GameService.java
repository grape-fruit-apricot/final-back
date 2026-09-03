package com.kh.midpoint.game.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.game.model.dto.GameResponse;
import com.kh.midpoint.game.model.vo.Game;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
	private final GameRuntimeStore runtimeStore;
	private final GameParticipantService gameParticipantService;
	
	@Transactional(noRollbackFor = NotFoundException.class)
	public GameResponse findGame(Long gameId, Long participantId) {
		Game game = validateMember(findRequiredGame(gameId), participantId);
        runtimeStore.expireTurnIfNeeded(gameId);
        delegateTimedOutHostIfNeeded(game);
        gameParticipantService.insertFinishedGame(gameId, game.getPlayers(), runtimeStore.get(gameId));
        delegateHostIfNeeded(game);
		return response(game, participantId);
	}

	private GameResponse response(Game game, Long participantId) {
		// TODO Auto-generated method stub
		return null;
	}

	private void delegateHostIfNeeded(Game game) {
		// TODO Auto-generated method stub
		
	}

	private void delegateTimedOutHostIfNeeded(Game game) {
		// TODO Auto-generated method stub
		
	}

	private Game validateMember(Object requiredGame, Long participantId) {
		// TODO Auto-generated method stub
		return null;
	}

	private Object findRequiredGame(Long gameId) {
		// TODO Auto-generated method stub
		return null;
	}

}

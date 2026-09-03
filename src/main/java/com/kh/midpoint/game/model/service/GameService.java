package com.kh.midpoint.game.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.game.model.dto.GameDto;
import com.kh.midpoint.game.model.dto.GameResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
	private final GameRuntimeStore runtimeStore;
	private final GameParticipantService gameParticipantService;
	
	@Transactional(noRollbackFor = NotFoundException.class)
    public GameResponse findGame(String roomUuid, Long participantId) {
        GameDto game = validateMember(findRequiredGame(roomUuid), participantId);
        Long roomId = game.getRoomId();
        runtimeStore.expireTurnIfNeeded(roomId);
        delegateTimedOutHostIfNeeded(game);
        gameParticipantService.insertFinishedGame(game.getPlayers(), runtimeStore.get(roomId));
        delegateHostIfNeeded(game);
        return response(game, participantId);
    }

	private GameResponse response(GameDto game, Long participantId) {
		// TODO Auto-generated method stub
		return null;
	}

	private void delegateHostIfNeeded(GameDto game) {
		// TODO Auto-generated method stub
		
	}

	private void delegateTimedOutHostIfNeeded(GameDto game) {
		// TODO Auto-generated method stub
		
	}

	private GameDto validateMember(Object requiredGame, Long participantId) {
		// TODO Auto-generated method stub
		return null;
	}

	private Object findRequiredGame(String roomUuid) {
		// TODO Auto-generated method stub
		return null;
	}

}

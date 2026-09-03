package com.kh.midpoint.game.model.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.DuplicateException;
import com.kh.midpoint.common.exception.ForbiddenException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.game.model.dao.GameMapper;
import com.kh.midpoint.game.model.dto.GameParticipantQueryResponseDto;
import com.kh.midpoint.game.model.dto.GameParticipantResponseDto;
import com.kh.midpoint.game.model.dto.GameRoomQueryResponseDto;
import com.kh.midpoint.game.model.dto.GameStateResponseDto;
import com.kh.midpoint.game.websocket.GameStatePublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
	private final GameRuntimeStore runtimeStore;
	private final GameParticipantService gameParticipantService;
	private final GameStatePublisher gameStatePublisher;
	
	 @Transactional
	    public GameStateResponseDto updateGameState(
	            String roomUuid,
	            Long participantId,
	            long version,
	            Map<String, Object> state) {
	        GameRoomQueryResponseDto game = findRequiredGame(roomUuid);
	        validateMember(game, participantId);

	        Long roomId = game.getRoomId();
	        runtimeStore.expireTurnIfNeeded(roomId);
	        if (!runtimeStore.gameStarted(roomId)) {
	            validateHost(game, participantId);
	        }

	        if (runtimeStore.update(roomId, version, state) == null) {
	            throw new DuplicateException("다른 참가자가 먼저 상태를 변경했습니다.");
	        }

	        gameParticipantService.insertFinishedGame(
	                game.getPlayers(),
	                runtimeStore.get(roomId));
	        gameStatePublisher.broadcast(roomUuid, runtimeStore.get(roomId));
	        return response(game, participantId);
	    }

	 	private GameStateResponseDto response(
	            GameRoomQueryResponseDto game,
	            Long participantId) {
	        var runtime = runtimeStore.get(game.getRoomId());
	        List<GameParticipantResponseDto> players = game.getPlayers().stream()
	                .map(player -> new GameParticipantResponseDto(
	                        player.getParticipantId(),
	                        player.getPlayerName(),
	                        player.getParticipantId().equals(game.getHostParticipantId()),
	                        isActive(player)))
	                .toList();

	        Long hostId = game.getHostParticipantId();
	        return new GameStateResponseDto(
	                game.getRoomUuid(),
	                participantId,
	                hostId,
	                participantId.equals(hostId),
	                players,
	                runtime.gameState(),
	                runtime.version());
	    }
	 	
	 private boolean isActive(GameParticipantQueryResponseDto player) {
	        return "ACTIVE".equals(player.getPlayerStatus());
	 }
	 	

	 private void validateHost(GameRoomQueryResponseDto game, Long participantId) {
		// TODO Auto-generated method stub
		
	}

	 private void validateMember(GameRoomQueryResponseDto game, Long participantId) {
		// TODO Auto-generated method stub
		
	}

	 private GameRoomQueryResponseDto findRequiredGame(String roomUuid) {
		// TODO Auto-generated method stub
		return null;
	 }
	
	
	
}

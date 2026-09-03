package com.kh.midpoint.game.model.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.ForbiddenException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.game.model.dao.GameMapper;
import com.kh.midpoint.game.model.dto.GameParticipantQueryResponseDto;
import com.kh.midpoint.game.model.dto.GameParticipantResponseDto;
import com.kh.midpoint.game.model.dto.GameRoomQueryResponseDto;
import com.kh.midpoint.game.model.dto.GameStateResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
	private final GameRuntimeStore runtimeStore;
	private final GameMapper gameMapper;
	
	@Transactional
    public GameStateResponseDto findGameState(String roomUuid, Long participantId) {
        GameRoomQueryResponseDto game = findRequiredGame(roomUuid);
        validateMember(game, participantId);
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

	private void validateMember(GameRoomQueryResponseDto game, Long participantId) {
        boolean member = game.getPlayers().stream()
                .anyMatch(player -> player.getParticipantId().equals(participantId)
                        && isActive(player));
        if (!member) {
            throw new ForbiddenException("이 방의 참가자가 아닙니다.");
        }
    }

	private GameRoomQueryResponseDto findRequiredGame(String roomUuid) {
		GameRoomQueryResponseDto game = gameMapper.findGameState(roomUuid);
        if (game == null) {
            throw new NotFoundException("존재하지 않는 방입니다.");
        }
        game.setPlayers(gameMapper.findPlayerList(game.getRoomId()));
        runtimeStore.participantsChanged(game.getRoomId(), activeIds(game));
        return game;
    }
	
	private boolean isActive(GameParticipantQueryResponseDto player) {
        return "ACTIVE".equals(player.getPlayerStatus());
    }
	
	private Set<String> activeIds(GameRoomQueryResponseDto game) {
        return game.getPlayers().stream()
                .filter(this::isActive)
                .map(player -> player.getParticipantId().toString())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
	
}

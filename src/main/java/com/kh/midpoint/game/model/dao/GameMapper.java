package com.kh.midpoint.game.model.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kh.midpoint.game.model.dto.GameParticipantQueryResponseDto;
import com.kh.midpoint.game.model.dto.GameRoomQueryResponseDto;

@Mapper
public interface GameMapper {
	GameRoomQueryResponseDto findGameState(String roomUuid);
	
	List<GameParticipantQueryResponseDto> findPlayerList(Long roomId);
}

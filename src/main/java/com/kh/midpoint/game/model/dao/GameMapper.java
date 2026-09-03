package com.kh.midpoint.game.model.dao;

import org.apache.ibatis.annotations.Mapper;

import com.kh.midpoint.game.model.dto.GameDto;

@Mapper
public interface GameMapper {
	GameDto findGame(String roomUuid);
}

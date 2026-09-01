package com.kh.midpoint.chat.model.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatMapper {

	String selectNicknameByParticipant(@Param("roomId") Long roomId, @Param("participantId") Long participantId);
	
}

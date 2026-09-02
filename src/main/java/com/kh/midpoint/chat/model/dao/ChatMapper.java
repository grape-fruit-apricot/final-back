package com.kh.midpoint.chat.model.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatMapper {

	String selectNicknameByRoomIdAndParticipantId(@Param("roomId") Long roomId,
			@Param("participantId") Long participantId);

}

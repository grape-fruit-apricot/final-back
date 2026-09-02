package com.kh.midpoint.chat.model.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kh.midpoint.chat.model.dto.ChatMessageResponseDto;
import com.kh.midpoint.chat.model.vo.ChatMessage;

@Mapper
public interface ChatMapper {

	String findNicknameByRoomIdAndParticipantId(@Param("roomId") Long roomId,
			@Param("participantId") Long participantId);

	/** 저장 후 message 의 messageId 에 채번된 값이 채워진다 */
	void insertMessage(ChatMessage message);

	ChatMessageResponseDto findMessage(Long messageId);

	List<ChatMessageResponseDto> findMessageList(Long roomId);

}

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

	void insertMessage(ChatMessage message);

	ChatMessageResponseDto findMessage(Long messageId);

	/** afterMessageId 가 null 이면 최근 100건, 값이 있으면 그 이후 메시지만 */
	List<ChatMessageResponseDto> findMessageList(@Param("roomId") Long roomId,
			@Param("afterMessageId") Long afterMessageId);

}

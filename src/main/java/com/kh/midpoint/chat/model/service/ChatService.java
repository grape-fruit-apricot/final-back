package com.kh.midpoint.chat.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.chat.model.dao.ChatMapper;
import com.kh.midpoint.chat.model.dto.ChatMessageResponseDto;
import com.kh.midpoint.chat.model.vo.ChatMessage;
import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.chat.model.vo.MsgType;
import com.kh.midpoint.common.exception.ForbiddenException;
import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

	private final ChatMapper chatMapper;
	private final RoomService roomService;

	@Transactional(readOnly = true)
	public ChatSession openSession(String roomUuid, Long participantId) {
		validateRequired(roomUuid, participantId);

		RoomResponseDto room = roomService.findRoom(roomUuid);

		String nickname = chatMapper.findNicknameByRoomIdAndParticipantId(room.getRoomId(), participantId);
		if (nickname == null) {
			throw new ForbiddenException("이 방의 참가자가 아닙니다.");
		}

		return new ChatSession(roomUuid, room.getRoomId(), participantId, nickname);
	}

	@Transactional
	public ChatMessageResponseDto saveMessage(ChatSession session, MsgType msgType, String content) {
		ChatMessage message = ChatMessage.builder()
				.roomId(session.roomId())
				.participantId(session.participantId())
				.content(content)
				.msgType(msgType)
				.build();

		chatMapper.insertMessage(message);

		return chatMapper.findMessage(message.getMessageId());
	}

	@Transactional(readOnly = true)
	public List<ChatMessageResponseDto> getMessages(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		return chatMapper.findMessageList(room.getRoomId());
	}

	private void validateRequired(String roomUuid, Long participantId) {
		if (roomUuid == null || roomUuid.isBlank()) {
			throw new InvalidStateException("roomUuid 가 필요합니다.");
		}
		if (participantId == null) {
			throw new InvalidStateException("participantId 가 필요합니다.");
		}
	}

}

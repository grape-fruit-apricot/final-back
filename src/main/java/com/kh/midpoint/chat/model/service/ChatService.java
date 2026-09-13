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
	public ChatSession findChatSession(String roomUuid, Long participantId) {
		validateRequired(roomUuid, participantId);

		RoomResponseDto room = roomService.findRoom(roomUuid);

		String nickname = chatMapper.findNicknameByRoomIdAndParticipantId(room.getRoomId(), participantId);
		if (nickname == null) {
			throw new ForbiddenException("이 방의 참가자가 아닙니다.");
		}

		return new ChatSession(roomUuid, room.getRoomId(), participantId, nickname);
	}

	// 참가자가 보낸 메시지를 저장한다. 앞뒤 공백을 걷어내고, 남는 내용이 없으면 저장하지 않는다.
	// 빈 메시지는 예외가 아니라 정상적인 입력 실수라 오류를 돌려주지 않고 null 로 알린다.
	@Transactional
	public ChatMessageResponseDto insertTalkMessage(ChatSession session, String content) {
		String trimmed = content == null ? "" : content.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		return insertMessage(session, MsgType.TALK, trimmed);
	}

	@Transactional
	public ChatMessageResponseDto insertMessage(ChatSession session, MsgType msgType, String content) {
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
	public List<ChatMessageResponseDto> findMessageList(String roomUuid, Long afterMessageId) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		return chatMapper.findMessageList(room.getRoomId(), afterMessageId);
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

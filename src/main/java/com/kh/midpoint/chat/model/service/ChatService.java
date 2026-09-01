package com.kh.midpoint.chat.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.chat.model.dao.ChatMapper;
import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.common.exception.ForbiddenException;
import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

	private final ChatMapper chatMapper;

	/**
	 * STOMP CONNECT 시점 검증.
	 *
	 * 방이 존재하고, 그 방의 참가자일 때만 세션 정보를 만들어 준다.
	 * 검증에 실패하면 예외를 던져 연결 자체를 거부한다.
	 *
	 * 참고: 여기서 던지는 예외는 HTTP 요청이 아니라 STOMP 채널에서 발생하므로
	 *       GlobalExceptionHandler 가 아니라 STOMP ERROR 프레임으로 클라이언트에 전달된다.
	 *       예외 타입은 의미를 맞추기 위해 공통 예외를 그대로 쓴다.
	 */
	@Transactional(readOnly = true)
	public ChatSession openSession(String roomUuid, Long participantId) {
		if (roomUuid == null || roomUuid.isBlank()) {
			throw new InvalidStateException("roomUuid 가 필요합니다.");
		}
		if (participantId == null) {
			throw new InvalidStateException("participantId 가 필요합니다.");
		}

		Long roomId = chatMapper.selectRoomIdByUuid(roomUuid);
		if (roomId == null) {
			throw new NotFoundException("존재하지 않는 방입니다: " + roomUuid);
		}

		// TODO: ROOM.EXPIRES_AT 이 지난 방을 막을지 정책 결정 후 반영
		// TODO: ROOM.STAGE 에 따라 채팅 가능 여부가 달라지는지 확인 후 반영

		String nickname = chatMapper.selectNicknameByParticipant(roomId, participantId);
		if (nickname == null) {
			throw new ForbiddenException("이 방의 참가자가 아닙니다.");
		}

		return new ChatSession(roomUuid, roomId, participantId, nickname);
	}

}

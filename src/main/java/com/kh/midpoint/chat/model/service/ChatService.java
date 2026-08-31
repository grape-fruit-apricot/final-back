package com.kh.midpoint.chat.model.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.chat.model.dao.ChatMapper;
import com.kh.midpoint.chat.model.dto.ChatMessageResponse;
import com.kh.midpoint.chat.model.vo.ChatMessage;
import com.kh.midpoint.chat.model.vo.ChatSession;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMapper chatMapper;

    /** roomUuid -> 현재 접속자 수. 메모리 관리라 서버 재시작 시 초기화된다. */
    private final Map<String, Integer> roomUserCount = new ConcurrentHashMap<>();

    /**
     * STOMP CONNECT 시점 검증.
     * 방이 존재하고 그 방의 참가자일 때만 세션 정보를 만들어 준다.
     */
    @Transactional(readOnly = true)
    public ChatSession openSession(String roomUuid, Long participantId) {
        if (roomUuid == null || participantId == null) {
            throw new IllegalArgumentException("roomUuid, participantId 는 필수입니다.");
        }

        Long roomId = chatMapper.selectRoomIdByUuid(roomUuid);
        if (roomId == null) {
            throw new IllegalArgumentException("존재하지 않는 방입니다.");
        }

        // TODO: ROOM.EXPIRES_AT 이 지난 방 차단 정책 정해지면 여기서 막을 것
        // TODO: ROOM.MAX_PARTICIPANTS 초과 여부도 여기서 검사할 수 있음

        String nickname = chatMapper.selectNicknameByParticipant(roomId, participantId);
        if (nickname == null) {
            throw new IllegalArgumentException("이 방의 참가자가 아닙니다.");
        }

        return new ChatSession(roomUuid, roomId, participantId, nickname);
    }

    /**
     * 메시지를 저장하고, 화면에 뿌릴 형태(닉네임 포함)로 되돌려준다.
     */
    @Transactional
    public ChatMessageResponse save(ChatSession session, String msgType, String content) {
        ChatMessage message = ChatMessage.builder()
                .roomId(session.roomId())
                .participantId(session.participantId())
                .content(content)
                .msgType(msgType)
                .build();

        chatMapper.insertMessage(message);

        return chatMapper.selectMessageById(message.getMessageId());
    }

    /** 방 입장 시 불러올 이전 대화 내역 (최근 100건) */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(String roomUuid) {
        Long roomId = chatMapper.selectRoomIdByUuid(roomUuid);
        if (roomId == null) {
            throw new IllegalArgumentException("존재하지 않는 방입니다.");
        }
        return chatMapper.selectMessagesByRoomId(roomId);
    }

    public void enterRoom(String roomUuid) {
        roomUserCount.merge(roomUuid, 1, Integer::sum);
    }

    public void leaveRoom(String roomUuid) {
        roomUserCount.computeIfPresent(roomUuid, (k, v) -> v <= 1 ? null : v - 1);
    }

    public int getUserCount(String roomUuid) {
        return roomUserCount.getOrDefault(roomUuid, 0);
    }
}

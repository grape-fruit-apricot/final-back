package com.kh.midpoint.chat.model.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.kh.midpoint.chat.model.dto.ChatMessageResponse;
import com.kh.midpoint.chat.model.vo.ChatMessage;

public interface ChatMapper {

    /** 대외용 ROOM_UUID -> 내부 ROOM_ID. 없으면 null */
    Long selectRoomIdByUuid(String roomUuid);

    /** 해당 방의 참가자면 닉네임을, 아니면 null 을 반환 (참가자 검증 겸용) */
    String selectNicknameByParticipant(@Param("roomId") Long roomId,
                                       @Param("participantId") Long participantId);

    /** 저장 후 message 의 messageId 에 채번된 값이 채워진다 */
    void insertMessage(ChatMessage message);

    /** 닉네임까지 JOIN 된 단건 조회 */
    ChatMessageResponse selectMessageById(Long messageId);

    /** 방의 최근 대화 100건 */
    List<ChatMessageResponse> selectMessagesByRoomId(Long roomId);
}

package com.kh.midpoint.chat.model.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatMapper {

	/**
	 * 대외용 ROOM_UUID 를 내부 ROOM_ID 로 바꾼다. 방이 없으면 null.
	 *
	 * TODO: ROOM 을 읽는 곳이 RoomMapper 와 둘로 나뉘었다.
	 *       RoomService 가 roomId 를 노출하게 되면 그쪽으로 합칠 것.
	 */
	Long selectRoomIdByUuid(String roomUuid);

	/**
	 * 해당 방의 참가자면 닉네임을, 아니면 null 을 반환한다.
	 * 참가자 검증과 닉네임 조회를 겸한다.
	 *
	 * TODO: 방 참가 기능이 완성되면 LEFT_AT 조건을 넣을지 정할 것.
	 */
	String selectNicknameByParticipant(@Param("roomId") Long roomId,
	                                   @Param("participantId") Long participantId);

}

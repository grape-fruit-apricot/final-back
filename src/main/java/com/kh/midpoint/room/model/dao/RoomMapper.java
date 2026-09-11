package com.kh.midpoint.room.model.dao;

import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.vo.Room;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoomMapper {

	void insertRoom(Room room);

	int updateMidpoint(Room room);

	int updateStage(Room room);

	RoomResponseDto findRoom(String roomUuid);

	// 방 행을 잠가서 읽는다. 같은 방에 동시에 들어온 변경을 한 줄로 세우는 지점이다.
	RoomResponseDto findRoomForUpdate(String roomUuid);

	// 만료 시각이 지난 방의 ID 를 상한 개수만큼 읽는다. 만료 판정은 DB 시계로 한다.
	List<Long> findExpiredRoomIdList(int batchSize);

	// 방을 지우면 참가자·식당·경로·추적 이력이 FK 의 ON DELETE CASCADE 로 함께 사라진다.
	int deleteRoomList(List<Long> roomIds);

}

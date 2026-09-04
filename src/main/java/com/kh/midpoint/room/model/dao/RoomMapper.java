package com.kh.midpoint.room.model.dao;

import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.vo.Room;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoomMapper {

	void insertRoom(Room room);

	int updateMidpoint(Room room);

	int updateStage(Room room);

	RoomResponseDto findRoom(String roomUuid);

	// 방 행을 잠가서 읽는다. 같은 방에 동시에 들어온 변경을 한 줄로 세우는 지점이다.
	RoomResponseDto findRoomForUpdate(String roomUuid);

}

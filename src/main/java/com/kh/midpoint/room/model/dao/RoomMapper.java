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

}

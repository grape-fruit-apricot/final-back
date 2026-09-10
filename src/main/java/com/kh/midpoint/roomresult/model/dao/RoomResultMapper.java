package com.kh.midpoint.roomresult.model.dao;

import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.roomresult.model.vo.RoomResult;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoomResultMapper {

	void insertRoomResult(RoomResult roomResult);

	RestaurantResponseDto findRoomResult(Long roomId);

}

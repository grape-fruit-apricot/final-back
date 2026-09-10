package com.kh.midpoint.roomresult.model.service;

import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.roomresult.model.dao.RoomResultMapper;
import com.kh.midpoint.roomresult.model.vo.RoomResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomResultService {

	private final RoomResultMapper roomResultMapper;

	@Transactional
	public void insertRoomResult(RoomResult roomResult) {
		roomResultMapper.insertRoomResult(roomResult);
	}

	@Transactional(readOnly = true)
	public RestaurantResponseDto findRoomResult(Long roomId) {
		return roomResultMapper.findRoomResult(roomId);
	}

}

package com.kh.midpoint.restaurant.model.service;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.restaurant.model.dao.RestaurantMapper;
import com.kh.midpoint.restaurant.model.dto.RestaurantCreateRequestDto;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.restaurant.model.vo.Restaurant;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

	private final RestaurantMapper restaurantMapper;
	private final RoomService roomService;

	@Transactional
	public void insertRestaurant(String roomUuid, RestaurantCreateRequestDto requestDto) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		if (!"MIDPOINT_FOUND".equals(room.getStage())) {
			throw new InvalidStateException("중간 지점이 결정된 상태에서만 식당을 등록할 수 있습니다.");
		}

		Restaurant restaurant = Restaurant.builder()
			.roomUuid(roomUuid)
			.participantId(requestDto.getParticipantId())
			.name(requestDto.getName())
			.address(requestDto.getAddress())
			.lat(requestDto.getLat())
			.lng(requestDto.getLng())
			.build();
		restaurantMapper.insertRestaurant(restaurant);
	}

	@Transactional(readOnly = true)
	public List<RestaurantResponseDto> findRestaurantList(String roomUuid) {
		roomService.findRoom(roomUuid);
		return restaurantMapper.findRestaurantList(roomUuid);
	}

}

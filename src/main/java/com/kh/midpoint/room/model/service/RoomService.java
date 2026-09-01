package com.kh.midpoint.room.model.service;

import com.kh.midpoint.room.model.dto.RoomCreateRequestDto;
import com.kh.midpoint.room.model.dto.RoomCreateResponseDto;
import com.kh.midpoint.room.model.dao.RoomMapper;
import com.kh.midpoint.room.model.vo.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {

	private final RoomMapper roomMapper;

	@Transactional
	public RoomCreateResponseDto insertRoom(RoomCreateRequestDto requestDto) {
		Room room = Room.create(requestDto.getMaxParticipants());
		roomMapper.insertRoom(room);
		return roomMapper.findRoom(room.getRoomUuid());
	}

}

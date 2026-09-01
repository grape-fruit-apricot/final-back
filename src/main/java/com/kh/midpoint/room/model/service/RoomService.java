package com.kh.midpoint.room.model.service;

import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.room.model.dto.RoomCreateRequestDto;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
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
	public RoomResponseDto insertRoom(RoomCreateRequestDto requestDto) {
		Room room = Room.create(requestDto.getMaxParticipants());
		roomMapper.insertRoom(room);
		return roomMapper.findRoom(room.getRoomUuid());
	}

	@Transactional(readOnly = true)
	public RoomResponseDto findRoom(String roomUuid) {
		RoomResponseDto responseDto = roomMapper.findRoom(roomUuid);
		if (responseDto == null) {
			throw new NotFoundException("존재하지 않는 방입니다: " + roomUuid);
		}
		return responseDto;
	}

}

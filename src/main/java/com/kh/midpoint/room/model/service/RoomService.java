package com.kh.midpoint.room.model.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.room.model.dao.RoomMapper;
import com.kh.midpoint.room.model.dto.RoomCreateRequestDto;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.vo.Room;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	@Transactional
	public void updateMidpoint(Long roomId, Double lat, Double lng, String source) {
		Room room = Room.builder()
				.roomId(roomId)
				.midpointLat(lat)
				.midpointLng(lng)
				.midpointSource(source)
				.build();
		int updatedRows = roomMapper.updateMidpoint(room);
		validateUpdatedRoom(updatedRows);
	}

	@Transactional
	public void updateStage(Long roomId, String stage) {
		validateStage(stage);
		Room room = Room.builder().roomId(roomId).stage(stage).build();
		int updatedRows = roomMapper.updateStage(room);
		validateUpdatedRoom(updatedRows);
	}

	private void validateStage(String stage) {
		if (!"WAITING".equals(stage)
				&& !"MODE_SELECTED".equals(stage)
				&& !"MIDPOINT_FOUND".equals(stage)
				&& !"RESOLVING".equals(stage)
				&& !"GAME_PLAYING".equals(stage)
				&& !"RESOLVED".equals(stage)) {
			throw new InvalidStateException("올바르지 않은 방 상태입니다: " + stage);
		}
	}

	private void validateUpdatedRoom(int updatedRows) {
		if (updatedRows == 0) {
			throw new NotFoundException("수정할 방을 찾을 수 없습니다.");
		}
	}

}

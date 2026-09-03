package com.kh.midpoint.point.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.room.model.dao.RoomMapper;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.room.model.vo.Room;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MidPointService {
	
	private final MidPointFinder midpointFinder;
	private final ParticipantService participantService;
	private final RoomService roomService;
	private final RoomMapper roomMapper;

	@Transactional
	public NearbyStationDto findMidpoint(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
	    List<ParticipantResponseDto> participants = participantService.findAllParticipants(roomUuid);
	    NearbyStationDto midpoint = midpointFinder.findMidPoint(participants);

	    Room updateRoom = Room.builder()
	    		.roomId(room.getRoomId())
	    		.midpointLat(midpoint.getLat())
	    		.midpointLng(midpoint.getLng())
	    		.midpointSource("KAKAO")
	    		.build();

	    int updatedRows = roomMapper.updateMidpoint(updateRoom);
	    validateUpdatedMidpoint(updatedRows);
	    roomService.updateStage(room.getRoomId(), "MIDPOINT_FOUND");

	    return midpoint;
	}

	private void validateUpdatedMidpoint(int updatedRows) {
		if (updatedRows == 0) {
			throw new NotFoundException("중간지점을 저장할 방을 찾을 수 없습니다.");
		}
	}
	
}


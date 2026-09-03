package com.kh.midpoint.point.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MidPointService {

	private final MidPointFinder midpointFinder;
	private final ParticipantService participantService;
	private final RoomService roomService;

	@Transactional
	public NearbyStationDto findMidpoint(String roomUuid, Long participantId) {
		participantService.validateHost(roomUuid, participantId);

		RoomResponseDto room = roomService.findRoom(roomUuid);
		validateMidpointNotFound(room);

		List<ParticipantResponseDto> participants = participantService.findAllParticipants(roomUuid);

		NearbyStationDto midpoint = midpointFinder.findMidPoint(participants);

		String source = midpoint.getName().equals(midpointFinder.getCenterName()) ? "FALLBACK" : "KAKAO";
		roomService.updateMidpoint(room.getRoomId(), midpoint.getLat(), midpoint.getLng(), source);
		roomService.updateStage(room.getRoomId(), "MIDPOINT_FOUND");

		return midpoint;
	}

	// stage 값을 열거하는 대신 좌표 유무로 판단한다. 좌표와 stage 는 같은 트랜잭션에서
	// 함께 갱신되므로 결과가 같고, 이후 MODE_SELECTED 같은 단계가 생겨도 영향을 받지 않는다.
	private void validateMidpointNotFound(RoomResponseDto room) {
		if (room.getMidpointLat() != null) {
			throw new InvalidStateException("이미 중간지점을 찾은 방입니다.");
		}
	}

}

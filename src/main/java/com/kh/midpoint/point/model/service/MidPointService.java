package com.kh.midpoint.point.model.service;

import java.util.List;

import org.springframework.stereotype.Service;

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

	// 여기에는 @Transactional 을 붙이지 않는다. 중간지점 계산은 카카오 1회 + Tmap 을
	// (후보 수 x 참가자 수)만큼 호출하므로, 트랜잭션 안에서 돌리면 그 시간 내내 DB 커넥션을
	// 붙잡아 관계없는 요청까지 커넥션 대기로 죽는다. 저장은 아래 두 updateXxx 가 각자
	// 트랜잭션을 열어 처리한다.
	public NearbyStationDto findMidpoint(String roomUuid, Long participantId) {
		participantService.validateHost(roomUuid, participantId);

		RoomResponseDto room = roomService.findRoom(roomUuid);
		validateMidpointNotFound(room);

		List<ParticipantResponseDto> participants = participantService.findParticipantList(roomUuid);

		NearbyStationDto midpoint = midpointFinder.findMidPoint(participants);

		String source = midpoint.getName().equals(midpointFinder.getCenterName()) ? "FALLBACK" : "KAKAO";
		roomService.updateMidpoint(room.getRoomId(), midpoint.getLat(), midpoint.getLng(), source);
		roomService.updateStage(room.getRoomId(), "MIDPOINT_FOUND");

		return midpoint;
	}

	// stage 값을 열거하는 대신 좌표 유무로 판단한다. 좌표가 먼저 저장되므로 이 검사만으로
	// 재실행을 막을 수 있고, 이후 MODE_SELECTED 같은 단계가 생겨도 영향을 받지 않는다.
	private void validateMidpointNotFound(RoomResponseDto room) {
		if (room.getMidpointLat() != null) {
			throw new InvalidStateException("이미 중간지점을 찾은 방입니다.");
		}
	}

}

package com.kh.midpoint.point.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.room.model.service.RoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MidPointService {

	private final MidPointFinder midpointFinder;
	private final ParticipantService participantService;
	private final RoomService roomService;

	@Transactional
	public NearbyStationDto findMidpoint(String roomUuid) {
	    List<ParticipantResponseDto> participants = participantService.findAllParticipants(roomUuid);

	    NearbyStationDto midpoint = midpointFinder.findMidPoint(participants);

	    String source = midpoint.getName().equals(midpointFinder.getCenterName()) ? "FALLBACK" : "KAKAO";
	    roomService.updateMidpoint(roomUuid, midpoint.getLat(), midpoint.getLng(), source);

	    return midpoint;
	}

}


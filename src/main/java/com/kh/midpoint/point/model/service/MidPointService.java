package com.kh.midpoint.point.model.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MidPointService {
	
	private final MidPointFinder midpointFinder;
	private final ParticipantService participantService;

	@Transactional
	public NearbyStationDto findMidpoint(String roomUuid) {
	    List<ParticipantResponseDto> participants = participantService.findAllParticipants(roomUuid);

	    return midpointFinder.findMidPoint(participants);
	}
	
}


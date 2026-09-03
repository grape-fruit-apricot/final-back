package com.kh.midpoint.point.model.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.kakao.KakaoLocalClient;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MidPointFinder {
	private final KakaoLocalClient kakaoLocalClient;
	private final WalkMidPointService walkMidPointService;
	
	@Value("${candidate.count}")
	private int stationCount;
	
	@Value("${candidate.name}")
	private String centerName;

	public NearbyStationDto findMidPoint(List<ParticipantResponseDto> participants) {
		validParticipant(participants);

		double centroidLat = participants.stream()
				.mapToDouble(ParticipantResponseDto::getPrefLat)
				.average()
				.orElseThrow(() -> new NotFoundException("참여자 위치 정보가 없습니다."));

		double centroidLng = participants.stream()
				.mapToDouble(ParticipantResponseDto::getPrefLng)
				.average()
				.orElseThrow(() -> new NotFoundException("참여자 위치 정보가 없습니다."));

		List<NearbyStationDto> stations = kakaoLocalClient.findNearbySubwayStations(centroidLng, centroidLat, stationCount);

		List<NearbyStationDto> candidates = new ArrayList<>(stations);
		candidates.add(new NearbyStationDto(centerName, centroidLat, centroidLng));

		return walkMidPointService.pickBest(participants, candidates);
	}
	
	public String getCenterName() {
		return centerName;
	}

	private void validParticipant(List<ParticipantResponseDto> participants) {
		if (participants == null || participants.isEmpty()) {
			throw new NotFoundException("참여자가 없습니다.");
		}
	}

}

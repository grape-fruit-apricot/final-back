package com.kh.midpoint.point.model.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.external.tmap.TmapRouteClient;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalkMidPointService {

	private final TmapRouteClient tmapRouteClient;

	public NearbyStationDto pickBest(List<ParticipantResponseDto> participants, List<NearbyStationDto> candidates) {
		NearbyStationDto best = null;
		int bestMaxMinutes = Integer.MAX_VALUE;

		for (NearbyStationDto candidate : candidates) {

			Integer maxMinutes = getMaxWalkingMinutes(participants, candidate);

			if (maxMinutes != null
					&& maxMinutes < bestMaxMinutes) {

				bestMaxMinutes = maxMinutes;
				best = candidate;
			}
		}

		validateBest(best);

		return best;
	}

	// 후보 전부에서 참가자 한 명이라도 도보 경로를 못 찾으면 best 가 null 로 남는다.
	// 그대로 반환하면 호출부에서 NPE 가 나므로 여기서 걸러낸다.
	private void validateBest(NearbyStationDto best) {
		if (best == null) {
			throw new NotFoundException("모든 후보 지점에서 도보 경로를 찾지 못했습니다.");
		}
	}

	private Integer getMaxWalkingMinutes(List<ParticipantResponseDto> participants, NearbyStationDto candidate) {
		int maxMinutes = 0;

		for (ParticipantResponseDto participant : participants) {

			Integer minutes = getWalkingMinutesOrNull(participant, candidate);

			if (minutes == null) {
				return null;
			}

			maxMinutes = Math.max(maxMinutes, minutes);
		}

		return maxMinutes;
	}

	private Integer getWalkingMinutesOrNull(ParticipantResponseDto participant, NearbyStationDto candidate) {
		try {
			return tmapRouteClient.getPedestrianRoute(
					participant.getPrefLng(),
					participant.getPrefLat(),
					candidate.getLng(),
					candidate.getLat())
					.getTimeMinutes();

		} catch (NotFoundException e) {
			return null;
		}
	}

}



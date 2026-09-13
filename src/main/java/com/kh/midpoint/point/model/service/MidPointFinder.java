package com.kh.midpoint.point.model.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.kakao.KakaoLocalClient;
import com.kh.midpoint.external.kakao.KakaoTransitClient;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MidPointFinder {
	private final KakaoLocalClient kakaoLocalClient;
	private final KakaoTransitClient kakaoTransitClient;
	private final WalkMidPointService walkMidPointService;
	
	@Value("${candidate.count}")
	private int stationCount;
	
	@Value("${candidate.name}")
	private String centerName;

	@Value("${candidate.walk.offset-meters}")
	private double walkOffsetMeters;

	@Value("${route.earth-radius-meters}")
	private double earthRadiusMeters;

	@Value("${route.mode.walk}")
	private String walkMode;

	@Value("${route.mode.transit}")
	private String transitMode;

	public NearbyStationDto findMidPoint(List<ParticipantResponseDto> participants, String travelMode) {
		validateParticipantList(participants);
		validateTravelMode(travelMode);

		double centroidLat = participants.stream()
				.mapToDouble(ParticipantResponseDto::getPrefLat)
				.average()
				.orElseThrow(() -> new NotFoundException("참여자 위치 정보가 없습니다."));

		double centroidLng = participants.stream()
				.mapToDouble(ParticipantResponseDto::getPrefLng)
				.average()
				.orElseThrow(() -> new NotFoundException("참여자 위치 정보가 없습니다."));

		if (walkMode.equals(travelMode)) {
			return walkMidPointService.findBestCandidate(
					participants, findWalkCandidateList(centroidLat, centroidLng));
		}

		List<NearbyStationDto> stations = kakaoLocalClient.findNearbySubwayStations(
				centroidLng, centroidLat, stationCount);
		return findBestTransitCandidate(participants, stations);
	}

	public String findMidpointSource(String travelMode) {
		validateTravelMode(travelMode);
		return walkMode.equals(travelMode) ? "FALLBACK" : "KAKAO";
	}

	private List<NearbyStationDto> findWalkCandidateList(double centerLat, double centerLng) {
		double latitudeOffset = Math.toDegrees(walkOffsetMeters / earthRadiusMeters);
		double longitudeOffset = Math.toDegrees(
				walkOffsetMeters / (earthRadiusMeters * Math.cos(Math.toRadians(centerLat))));

		return List.of(
				new NearbyStationDto(centerName, centerLat, centerLng),
				new NearbyStationDto(centerName, centerLat + latitudeOffset, centerLng),
				new NearbyStationDto(centerName, centerLat - latitudeOffset, centerLng),
				new NearbyStationDto(centerName, centerLat, centerLng + longitudeOffset),
				new NearbyStationDto(centerName, centerLat, centerLng - longitudeOffset));
	}

	private NearbyStationDto findBestTransitCandidate(
			List<ParticipantResponseDto> participants, List<NearbyStationDto> candidates) {
		NearbyStationDto best = null;
		int bestMaxMinutes = Integer.MAX_VALUE;

		for (NearbyStationDto candidate : candidates) {
			Integer maxMinutes = findMaxTransitMinutes(participants, candidate);
			if (maxMinutes != null && maxMinutes < bestMaxMinutes) {
				best = candidate;
				bestMaxMinutes = maxMinutes;
			}
		}

		if (best == null) {
			throw new NotFoundException("중간지점을 찾지 못했습니다.");
		}
		return best;
	}

	private Integer findMaxTransitMinutes(
			List<ParticipantResponseDto> participants, NearbyStationDto candidate) {
		int maxMinutes = 0;
		for (ParticipantResponseDto participant : participants) {
			try {
				int minutes = kakaoTransitClient.findTransitRoute(
						participant.getPrefLng(), participant.getPrefLat(),
						candidate.getLng(), candidate.getLat())
						.getTimeMinutes();
				maxMinutes = Math.max(maxMinutes, minutes);
			} catch (NotFoundException e) {
				return null;
			}
		}
		return maxMinutes;
	}

	private void validateTravelMode(String travelMode) {
		if (!walkMode.equals(travelMode) && !transitMode.equals(travelMode)) {
			throw new InvalidStateException("이동수단은 WALK 또는 TRANSIT이어야 합니다.");
		}
	}

	private void validateParticipantList(List<ParticipantResponseDto> participants) {
		if (participants == null || participants.isEmpty()) {
			throw new NotFoundException("참여자가 없습니다.");
		}
	}

}

package com.kh.midpoint.point.model.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kh.midpoint.external.kakao.KakaoLocalClient;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MidPointFinder {

	private static final int CANDIDATE_STATION_COUNT = 3;
    private static final String CENTER_CANDIDATE_NAME = "중심점";

    private final KakaoLocalClient kakaoLocalClient;
    private final WalkMidPointService walkMidPointService;

    public NearbyStationDto findMidPoint(List<ParticipantResponseDto> participants) {
        double centroidLat = participants.stream()
                .mapToDouble(ParticipantResponseDto::getPrefLat)
                .average()
                .orElseThrow();

        double centroidLng = participants.stream()
                .mapToDouble(ParticipantResponseDto::getPrefLng)
                .average()
                .orElseThrow();

        List<NearbyStationDto> stations =
                kakaoLocalClient.findNearbySubwayStations(
                        centroidLng,
                        centroidLat,
                        CANDIDATE_STATION_COUNT
                );

        List<NearbyStationDto> candidates =
                new ArrayList<>(stations);

        candidates.add(
                new NearbyStationDto(
                        CENTER_CANDIDATE_NAME,
                        centroidLat,
                        centroidLng
                )
        );

        return walkMidPointService.pickBest(
                participants,
                candidates
        );
    }

}

package com.kh.midpoint.route.model.service;

import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.kakao.KakaoTransitClient;
import com.kh.midpoint.external.kakao.TransitRouteResponseDto;
import com.kh.midpoint.external.tmap.TmapRouteClient;
import com.kh.midpoint.external.tmap.TmapRouteDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.route.model.dao.RouteMapper;
import com.kh.midpoint.route.model.dto.ParticipantRouteQueryDto;
import com.kh.midpoint.route.model.dto.RoutePointDto;
import com.kh.midpoint.route.model.dto.RouteSegmentDto;
import com.kh.midpoint.route.model.vo.ParticipantRoute;
import com.kh.midpoint.route.model.vo.ParticipantRoutePoint;
import com.kh.midpoint.route.model.vo.ParticipantRouteSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 외부 API 를 불러 참가자별 경로를 만들고 저장한다. 결과를 정하지도, 응답을 조립하지도 않는다.
// RouteService 가 확정·수집·조립 셋을 겸하고 있어서 수집 부분만 떼어냈다.
//
// 외부 API 호출은 트랜잭션 밖에서 한다. 참가자 수 x 이동수단 2 만큼 부르므로
// 트랜잭션 안에서 돌리면 그 시간 내내 DB 커넥션을 붙잡아 관계없는 요청까지 대기에 걸린다.
// 저장은 경로 한 건 단위로 짧게 끊는다.
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteComposer {

	@Value("${route.mode.walk}")
	private String walkMode;

	@Value("${route.mode.transit}")
	private String transitMode;

	private final RouteMapper routeMapper;
	private final TmapRouteClient tmapRouteClient;
	private final KakaoTransitClient kakaoTransitClient;
	private final TransactionTemplate transactionTemplate;

	// 저장을 마친 뒤의 경로 목록을 돌려준다. 새로 넣은 행의 ROUTE_ID 는 알 수 없어 다시 조회해야
	// 하지만, 이미 확정된 방을 다시 부르는 경우에는 처음 조회한 결과를 그대로 재사용한다.
	public List<ParticipantRouteQueryDto> insertMissingRouteList(Long roomId, List<ParticipantResponseDto> participants, RestaurantResponseDto restaurant) {
		List<ParticipantRouteQueryDto> savedRoutes = routeMapper.findRouteList(roomId, null);
		Set<String> savedRouteKeys = savedRoutes.stream()
				.map(route -> findRouteKey(route.getParticipantId(), route.getTravelMode()))
				.collect(Collectors.toCollection(HashSet::new));

		for (ParticipantResponseDto participant : participants) {
			insertMissingRoute(roomId, participant, restaurant, walkMode, savedRouteKeys);
			insertMissingRoute(roomId, participant, restaurant, transitMode, savedRouteKeys);
		}

		return routeMapper.findRouteList(roomId, null);
	}

	private void insertMissingRoute(Long roomId, ParticipantResponseDto participant, RestaurantResponseDto restaurant, String travelMode, Set<String> savedRouteKeys) {
		String routeKey = findRouteKey(participant.getParticipantId(), travelMode);
		if (savedRouteKeys.contains(routeKey)) {
			return;
		}

		try {
			TmapRouteDto route = travelMode.equals(walkMode)
					? findWalkRoute(participant, restaurant)
					: findTransitRoute(participant, restaurant);
			transactionTemplate.executeWithoutResult(status -> insertRoute(
					roomId, participant.getParticipantId(), travelMode, route));
			savedRouteKeys.add(routeKey);
		} catch (RuntimeException e) {
			log.warn("참가자 {} {} 경로 생성 실패", participant.getParticipantId(), travelMode, e);
		}
	}

	private TmapRouteDto findWalkRoute(ParticipantResponseDto participant, RestaurantResponseDto restaurant) {
		return tmapRouteClient.getPedestrianRoute(
				participant.getPrefLng(), participant.getPrefLat(),
				restaurant.getLng(), restaurant.getLat());
	}

	private TmapRouteDto findTransitRoute(ParticipantResponseDto participant, RestaurantResponseDto restaurant) {
		TransitRouteResponseDto transitRoute = kakaoTransitClient.findTransitRoute(
				participant.getPrefLng(), participant.getPrefLat(),
				restaurant.getLng(), restaurant.getLat());
		if (transitRoute.getPoints().isEmpty()) {
			throw new NotFoundException("대중교통 경로 좌표를 찾지 못했습니다.");
		}

		List<RouteSegmentDto> segments = new ArrayList<>();
		int timeMinutes = transitRoute.getTimeMinutes();

		RoutePointDto boardingPoint = transitRoute.getPoints().get(0);
		TmapRouteDto boardingRoute = tmapRouteClient.getPedestrianRoute(
				participant.getPrefLng(), participant.getPrefLat(),
				boardingPoint.getLng(), boardingPoint.getLat());
		timeMinutes += boardingRoute.getTimeMinutes();
		addRouteSegmentList(segments, boardingRoute.getSegments());

		addRouteSegmentList(segments, transitRoute.getSegments());

		RoutePointDto alightingPoint = transitRoute.getPoints()
				.get(transitRoute.getPoints().size() - 1);
		TmapRouteDto destinationRoute = tmapRouteClient.getPedestrianRoute(
				alightingPoint.getLng(), alightingPoint.getLat(),
				restaurant.getLng(), restaurant.getLat());
		timeMinutes += destinationRoute.getTimeMinutes();
		addRouteSegmentList(segments, destinationRoute.getSegments());

		List<RoutePointDto> points = segments.stream()
				.flatMap(segment -> segment.getPoints().stream())
				.toList();
		return new TmapRouteDto(timeMinutes, points, segments);
	}

	private void addRouteSegmentList(List<RouteSegmentDto> target,
			List<RouteSegmentDto> source) {
		for (RouteSegmentDto segment : source) {
			target.add(new RouteSegmentDto(
					target.size(), segment.getSegmentType(), segment.getTimeMinutes(),
					segment.getGuidance(), segment.getVehicles(), segment.getPoints()));
		}
	}

	// 경로 한 건과 그 구간·좌표를 한 트랜잭션 안에서 넣는다. 매퍼가 시퀀스를 인라인으로 쓰고
	// 부모-자식을 CURRVAL 로 잇기 때문에, 이 메서드가 중간에 다른 경로를 끼워 넣으면 안 된다.
	private void insertRoute(Long roomId, Long participantId, String travelMode, TmapRouteDto route) {
		ParticipantRoute participantRoute = ParticipantRoute.builder()
				.roomId(roomId)
				.participantId(participantId)
				.travelMode(travelMode)
				.timeMinutes(route.getTimeMinutes())
				.build();
		routeMapper.insertRoute(participantRoute);

		for (RouteSegmentDto segment : route.getSegments()) {
			ParticipantRouteSegment participantRouteSegment = ParticipantRouteSegment.builder()
					.segmentOrder(segment.getSegmentOrder())
					.segmentType(segment.getSegmentType())
					.timeMinutes(segment.getTimeMinutes())
					.guidance(segment.getGuidance())
					.vehicles(findVehicleText(segment.getVehicles()))
					.build();
			routeMapper.insertRouteSegment(participantRouteSegment);

			List<ParticipantRoutePoint> routePoints = new ArrayList<>();
			int pointOrder = 0;
			for (RoutePointDto point : segment.getPoints()) {
				routePoints.add(ParticipantRoutePoint.builder()
						.pointOrder(pointOrder)
						.lat(point.getLat())
						.lng(point.getLng())
						.build());
				pointOrder++;
			}
			if (!routePoints.isEmpty()) {
				routeMapper.insertRoutePointList(routePoints);
			}
		}
	}

	private String findVehicleText(List<String> vehicles) {
		return vehicles == null || vehicles.isEmpty() ? null : String.join(",", vehicles);
	}

	private String findRouteKey(Long participantId, String travelMode) {
		return participantId + ":" + travelMode;
	}
}

package com.kh.midpoint.route.model.service;

import com.kh.midpoint.room.model.vo.RoomStage;
import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.kakao.KakaoTransitClient;
import com.kh.midpoint.external.kakao.TransitRouteResponseDto;
import com.kh.midpoint.external.tmap.TmapRouteClient;
import com.kh.midpoint.external.tmap.TmapRouteDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.roomresult.model.service.RoomResultService;
import com.kh.midpoint.route.model.dao.RouteMapper;
import com.kh.midpoint.route.model.dto.ParticipantRouteQueryDto;
import com.kh.midpoint.route.model.dto.RoutePointDto;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.route.model.dto.RouteSegmentDto;
import com.kh.midpoint.route.model.vo.ParticipantRoute;
import com.kh.midpoint.route.model.vo.ParticipantRoutePoint;
import com.kh.midpoint.route.model.vo.ParticipantRouteSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

	@Value("${route.mode.walk}")
	private String walkMode;

	@Value("${route.mode.transit}")
	private String transitMode;

	private final RoomService roomService;
	private final ParticipantService participantService;
	private final RoomResultService roomResultService;
	private final RouteMapper routeMapper;
	private final RouteAssembler routeAssembler;
	private final RoomResultResolver roomResultResolver;
	private final TmapRouteClient tmapRouteClient;
	private final KakaoTransitClient kakaoTransitClient;
	private final TransactionTemplate transactionTemplate;

	// 참가자별 외부 경로 API 호출이 끝날 때까지 DB 트랜잭션이 유지되지 않도록
	// 외부 API 조회는 트랜잭션 밖에서 수행한다.
	// 경로와 좌표 저장은 참가자별 이동수단 하나의 단위로 짧게 처리한다.
	public RouteResponseDto insertRouteResult(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		roomResultResolver.validateGameNotPlaying(room.getStage());

		List<ParticipantResponseDto> participants = participantService.findParticipantList(roomUuid);
		RestaurantResponseDto restaurant = roomResultResolver.insertRoomResult(roomUuid, room.getRoomId());

		List<ParticipantRouteQueryDto> routes = insertMissingRouteList(room.getRoomId(), participants, restaurant);
		roomService.updateStage(room.getRoomId(), RoomStage.RESOLVED.name());

		return new RouteResponseDto(restaurant, routeAssembler.findParticipantRouteList(room.getRoomId(), routes));
	}

	// 이미 확정된 결과를 다시 계산하지 않고 읽기만 한다. 새로고침이나 뒤늦은 입장에서
	// insertRouteResult 를 다시 불러도 방·참가자·이동수단이 같은 기존 경로를 재사용한다.
	@Transactional(readOnly = true)
	public RouteResponseDto findRouteResult(String roomUuid, String travelMode) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		validateTravelMode(travelMode);

		RestaurantResponseDto restaurant = roomResultService.findRoomResult(room.getRoomId());
		if (restaurant == null) {
			throw new NotFoundException("아직 확정된 결과가 없습니다.");
		}

		List<ParticipantRouteQueryDto> routes = routeMapper.findRouteList(room.getRoomId(), travelMode);

		return new RouteResponseDto(restaurant, routeAssembler.findParticipantRouteList(room.getRoomId(), routes));
	}

	// 저장을 마친 뒤의 경로 목록을 돌려준다. 새로 넣은 행의 ROUTE_ID 는 알 수 없어 다시 조회해야
	// 하지만, 이미 확정된 방을 다시 부르는 경우에는 처음 조회한 결과를 그대로 재사용한다.
	private List<ParticipantRouteQueryDto> insertMissingRouteList(Long roomId, List<ParticipantResponseDto> participants, RestaurantResponseDto restaurant) {
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

	private void insertRoute(Long roomId, Long participantId, String travelMode, TmapRouteDto route) {
		Long routeId = routeMapper.findNextRouteId();
		ParticipantRoute participantRoute = ParticipantRoute.builder()
				.routeId(routeId)
				.roomId(roomId)
				.participantId(participantId)
				.travelMode(travelMode)
				.timeMinutes(route.getTimeMinutes())
				.build();
		routeMapper.insertRoute(participantRoute);

		for (RouteSegmentDto segment : route.getSegments()) {
			Long routeSegmentId = routeMapper.findNextRouteSegmentId();
			ParticipantRouteSegment participantRouteSegment = ParticipantRouteSegment.builder()
					.routeSegmentId(routeSegmentId)
					.routeId(routeId)
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
						.routeSegmentId(routeSegmentId)
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

	private void validateTravelMode(String travelMode) {
		if (!walkMode.equals(travelMode) && !transitMode.equals(travelMode)) {
			throw new InvalidStateException("이동수단은 WALK 또는 TRANSIT이어야 합니다.");
		}
	}

	private String findRouteKey(Long participantId, String travelMode) {
		return participantId + ":" + travelMode;
	}

}

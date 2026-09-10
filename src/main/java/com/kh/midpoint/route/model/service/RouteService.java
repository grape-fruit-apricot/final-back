package com.kh.midpoint.route.model.service;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.kakao.KakaoTransitClient;
import com.kh.midpoint.external.kakao.TransitRouteResponseDto;
import com.kh.midpoint.external.tmap.TmapRouteClient;
import com.kh.midpoint.external.tmap.TmapRouteDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.restaurant.model.service.RestaurantService;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.roomresult.model.service.RoomResultService;
import com.kh.midpoint.roomresult.model.vo.RoomResult;
import com.kh.midpoint.route.model.dao.RouteMapper;
import com.kh.midpoint.route.model.dto.ParticipantRouteQueryDto;
import com.kh.midpoint.route.model.dto.ParticipantRouteResponseDto;
import com.kh.midpoint.route.model.dto.RoutePointQueryDto;
import com.kh.midpoint.route.model.dto.RoutePointDto;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.route.model.dto.RouteSegmentDto;
import com.kh.midpoint.route.model.vo.ParticipantRoute;
import com.kh.midpoint.route.model.vo.ParticipantRoutePoint;
import com.kh.midpoint.route.model.vo.ParticipantRouteSegment;
import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.service.SelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
	private final SelectionService selectionService;
	private final RestaurantService restaurantService;
	private final RoomResultService roomResultService;
	private final RouteMapper routeMapper;
	private final TmapRouteClient tmapRouteClient;
	private final KakaoTransitClient kakaoTransitClient;
	private final TransactionTemplate transactionTemplate;

	// 참가자별 외부 경로 API 호출이 끝날 때까지 DB 트랜잭션이 유지되지 않도록
	// 외부 API 조회는 트랜잭션 밖에서 수행한다.
	// 경로와 좌표 저장은 참가자별 이동수단 하나의 단위로 짧게 처리한다.
	public RouteResponseDto findRoute(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		// 게임이 도는 중에는 결과를 확정하지 않는다. 이게 없으면 방장이 무작위 우회를 눌러
		// 참가자들이 주머니를 고르는 도중에 결과를 가로챌 수 있다.
		// 게임이 끝나거나 중단되면 RESOLVING 으로 돌아오므로 그때부터 확정할 수 있다.
		if ("GAME_PLAYING".equals(room.getStage())) {
			throw new InvalidStateException("게임이 진행 중입니다.");
		}

		List<ParticipantResponseDto> participants = participantService.findParticipantList(roomUuid);
		RestaurantResponseDto restaurant = findRestaurant(roomUuid, room.getRoomId());

		List<ParticipantRouteQueryDto> routes = insertMissingRouteList(room.getRoomId(), participants, restaurant);
		roomService.updateStage(room.getRoomId(), "RESOLVED");

		return new RouteResponseDto(restaurant, findParticipantRouteList(room.getRoomId(), routes));
	}

	// 이미 확정된 결과를 다시 계산하지 않고 읽기만 한다. 새로고침이나 뒤늦은 입장에서
	// findRoute 를 다시 불러도 방·참가자·이동수단이 같은 기존 경로를 재사용한다.
	@Transactional(readOnly = true)
	public RouteResponseDto findRouteResult(String roomUuid, String travelMode) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		validateTravelMode(travelMode);

		RestaurantResponseDto restaurant = roomResultService.findRoomResult(room.getRoomId());
		if (restaurant == null) {
			throw new NotFoundException("아직 확정된 결과가 없습니다.");
		}

		List<ParticipantRouteQueryDto> routes = routeMapper.findRouteList(room.getRoomId(), travelMode);

		return new RouteResponseDto(restaurant, findParticipantRouteList(room.getRoomId(), routes));
	}

	private RestaurantResponseDto findRestaurant(String roomUuid, Long roomId) {
		RestaurantResponseDto restaurant = roomResultService.findRoomResult(roomId);
		if (restaurant != null) {
			return restaurant;
		}

		Long restaurantId = findRandomRestaurantId(findSelectedRestaurantIdList(roomUuid));
		restaurant = restaurantService.findRestaurant(restaurantId);

		RoomResult roomResult = RoomResult.builder()
				.roomId(roomId)
				.restaurantId(restaurantId)
				.build();
		roomResultService.insertRoomResult(roomResult);

		return restaurant;
	}

	private List<Long> findSelectedRestaurantIdList(String roomUuid) {
		// 중복을 제거하지 않는다. 선택한 참가자 수만큼 후보에 들어가야
		// 식당이 아니라 사람 기준으로 확률이 같아진다(3명이 고른 식당이 3배 확률).
		// 1인 1선택(UK_SELECTION_PARTICIPANT)이라 이 목록이 곧 참가자별 선택이다.
		List<Long> restaurantIds = selectionService.findSelectionList(roomUuid).stream()
				.map(SelectionResponseDto::getRestaurantId)
				.toList();

		validateSelectedRestaurantIdList(restaurantIds);
		return restaurantIds;
	}

	private void validateSelectedRestaurantIdList(List<Long> restaurantIds) {
		if (restaurantIds.isEmpty()) {
			throw new InvalidStateException("식당 선택을 완료한 참가자가 없습니다.");
		}
	}

	private Long findRandomRestaurantId(List<Long> restaurantIds) {
		int index = ThreadLocalRandom.current().nextInt(restaurantIds.size());
		return restaurantIds.get(index);
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

	private List<ParticipantRouteResponseDto> findParticipantRouteList(Long roomId, List<ParticipantRouteQueryDto> routes) {
		// 경로마다 좌표를 따로 조회하면 참가자 수만큼 쿼리가 늘어난다. 도보 폴리라인은
		// 경로당 좌표가 수백 개라 방 단위로 한 번에 가져와 routeId 로 나눈다.
		Map<Long, List<RouteSegmentDto>> segmentsByRouteId = findSegmentsByRouteId(roomId);

		return routes.stream()
				.map(route -> toParticipantRouteResponse(route, segmentsByRouteId.getOrDefault(route.getRouteId(), List.of())))
				.toList();
	}

	private Map<Long, List<RouteSegmentDto>> findSegmentsByRouteId(Long roomId) {
		Map<Long, Map<Long, RouteSegmentDto>> segmentsByRouteAndId = new LinkedHashMap<>();
		for (RoutePointQueryDto point : routeMapper.findRoutePointListByRoom(roomId)) {
			RouteSegmentDto segment = segmentsByRouteAndId
					.computeIfAbsent(point.getRouteId(), routeId -> new LinkedHashMap<>())
					.computeIfAbsent(point.getRouteSegmentId(), routeSegmentId ->
							new RouteSegmentDto(
									point.getSegmentOrder(), point.getSegmentType(),
									point.getSegmentTimeMinutes(), point.getGuidance(),
									findVehicleList(point.getVehicles()), new ArrayList<>()));
			segment.getPoints().add(new RoutePointDto(point.getLat(), point.getLng()));
		}

		Map<Long, List<RouteSegmentDto>> segmentsByRouteId = new LinkedHashMap<>();
		segmentsByRouteAndId.forEach((routeId, segmentsById) ->
				segmentsByRouteId.put(routeId, new ArrayList<>(segmentsById.values())));
		return segmentsByRouteId;
	}

	private List<String> findVehicleList(String vehicles) {
		return vehicles == null || vehicles.isBlank()
				? List.of()
				: List.of(vehicles.split(","));
	}

	private ParticipantRouteResponseDto toParticipantRouteResponse(ParticipantRouteQueryDto route, List<RouteSegmentDto> segments) {
		List<RoutePointDto> points = segments.stream()
				.flatMap(segment -> segment.getPoints().stream())
				.toList();
		return new ParticipantRouteResponseDto(route.getParticipantId(), route.getNickname(), route.getTravelMode(), route.getTimeMinutes(), points, segments);
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

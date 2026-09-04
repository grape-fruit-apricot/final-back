package com.kh.midpoint.route.model.service;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.tmap.RoutePointDto;
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
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.route.model.vo.ParticipantRoute;
import com.kh.midpoint.route.model.vo.ParticipantRoutePoint;
import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.service.SelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
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

	private final RoomService roomService;
	private final ParticipantService participantService;
	private final SelectionService selectionService;
	private final RestaurantService restaurantService;
	private final RoomResultService roomResultService;
	private final RouteMapper routeMapper;
	private final TmapRouteClient tmapRouteClient;
	private final TransactionTemplate transactionTemplate;

	// 여기에는 @Transactional 을 붙이지 않는다. 참가자 수만큼 Tmap 을 순차 호출하므로
	// 트랜잭션으로 묶으면 그 시간 내내 DB 커넥션과 PARTICIPANT_ROUTE 잠금을 붙잡는다.
	// 저장은 참가자 1명 단위로 transactionTemplate 안에서 짧게 끊는다.
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
	// findRoute 를 다시 부르면 경로를 중복 저장하려다 UK_PART_ROUTE_PART 에 걸릴 수 있다.
	@Transactional(readOnly = true)
	public RouteResponseDto findRouteResult(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);

		RestaurantResponseDto restaurant = roomResultService.findRoomResult(room.getRoomId());
		if (restaurant == null) {
			throw new NotFoundException("아직 확정된 결과가 없습니다.");
		}

		List<ParticipantRouteQueryDto> routes = routeMapper.findRouteList(room.getRoomId());

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
	private List<ParticipantRouteQueryDto> insertMissingRouteList(Long roomId,
			List<ParticipantResponseDto> participants, RestaurantResponseDto restaurant) {
		List<ParticipantRouteQueryDto> savedRoutes = routeMapper.findRouteList(roomId);
		Set<Long> savedParticipantIds = savedRoutes.stream()
				.map(ParticipantRouteQueryDto::getParticipantId)
				.collect(Collectors.toSet());

		boolean inserted = false;
		for (ParticipantResponseDto participant : participants) {
			if (savedParticipantIds.contains(participant.getParticipantId())) {
				continue;
			}

			TmapRouteDto route = findParticipantRoute(participant, restaurant);
			if (route == null) {
				continue;
			}

			// 경로 1건과 그 좌표는 함께 저장돼야 한다. 외부 호출은 이 바깥에 두고 저장만 감싼다.
			transactionTemplate.executeWithoutResult(
					status -> insertRoute(roomId, participant.getParticipantId(), route));
			inserted = true;
		}

		return inserted ? routeMapper.findRouteList(roomId) : savedRoutes;
	}

	private TmapRouteDto findParticipantRoute(ParticipantResponseDto participant,
			RestaurantResponseDto restaurant) {
		try {
			return tmapRouteClient.getPedestrianRoute(
					participant.getPrefLng(), participant.getPrefLat(),
					restaurant.getLng(), restaurant.getLat());
		} catch (RuntimeException e) {
			log.warn("참가자 {} 도보 경로 조회 실패", participant.getParticipantId(), e);
			return null;
		}
	}

	private void insertRoute(Long roomId, Long participantId, TmapRouteDto route) {
		ParticipantRoute participantRoute = ParticipantRoute.builder()
				.roomId(roomId)
				.participantId(participantId)
				.timeMinutes(route.getTimeMinutes())
				.build();
		routeMapper.insertRoute(participantRoute);

		List<ParticipantRoutePoint> routePoints = new ArrayList<>();
		for (int index = 0; index < route.getPoints().size(); index++) {
			routePoints.add(ParticipantRoutePoint.builder()
					.participantId(participantId)
					.pointOrder(index)
					.lat(route.getPoints().get(index).getLat())
					.lng(route.getPoints().get(index).getLng())
					.build());
		}
		routeMapper.insertRoutePointList(routePoints);
	}

	private List<ParticipantRouteResponseDto> findParticipantRouteList(Long roomId,
			List<ParticipantRouteQueryDto> routes) {
		// 경로마다 좌표를 따로 조회하면 참가자 수만큼 쿼리가 늘어난다. 도보 폴리라인은
		// 경로당 좌표가 수백 개라 방 단위로 한 번에 가져와 routeId 로 나눈다.
		Map<Long, List<RoutePointDto>> pointsByRouteId = routeMapper.findRoutePointListByRoom(roomId).stream()
				.collect(Collectors.groupingBy(RoutePointQueryDto::getRouteId, LinkedHashMap::new,
						Collectors.mapping(point -> new RoutePointDto(point.getLat(), point.getLng()),
								Collectors.toList())));

		return routes.stream()
				.map(route -> new ParticipantRouteResponseDto(
						route.getParticipantId(),
						route.getNickname(),
						route.getTimeMinutes(),
						pointsByRouteId.getOrDefault(route.getRouteId(), List.of())))
				.toList();
	}

}

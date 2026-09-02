package com.kh.midpoint.route.model.service;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
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
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.route.model.vo.ParticipantRoute;
import com.kh.midpoint.route.model.vo.ParticipantRoutePoint;
import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.service.SelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

	@Transactional
	public RouteResponseDto findRoute(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		List<ParticipantResponseDto> participants = participantService.findAllParticipants(roomUuid);
		RestaurantResponseDto restaurant = findRestaurant(roomUuid, room.getRoomId(), participants);

		insertMissingRouteList(room.getRoomId(), participants, restaurant);
		roomService.updateStage(room.getRoomId(), "RESOLVED");

		return new RouteResponseDto(restaurant, findParticipantRouteList(room.getRoomId()));
	}

	private RestaurantResponseDto findRestaurant(String roomUuid, Long roomId,
			List<ParticipantResponseDto> participants) {
		RestaurantResponseDto restaurant = roomResultService.findRoomResult(roomId);
		if (restaurant != null) {
			return restaurant;
		}

		List<Long> restaurantIds = findSelectedRestaurantIdList(participants);
		Long restaurantId = findRandomRestaurantId(restaurantIds);
		restaurant = findRestaurant(roomUuid, restaurantId);

		RoomResult roomResult = RoomResult.builder()
				.roomId(roomId)
				.restaurantId(restaurantId)
				.build();
		roomResultService.insertRoomResult(roomResult);

		return restaurant;
	}

	private List<Long> findSelectedRestaurantIdList(
			List<ParticipantResponseDto> participants) {
		List<Long> restaurantIds = participants.stream()
				.map(participant -> selectionService.findSelection(participant.getParticipantId()))
				.filter(Objects::nonNull)
				.map(SelectionResponseDto::getRestaurantId)
				.distinct()
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

	private RestaurantResponseDto findRestaurant(String roomUuid, Long restaurantId) {
		return restaurantService.findRestaurantList(roomUuid).stream()
				.filter(item -> restaurantId.equals(item.getRestaurantId()))
				.findFirst()
				.orElseThrow(() -> new NotFoundException("선정된 식당을 찾을 수 없습니다: " + restaurantId));
	}

	private void insertMissingRouteList(Long roomId,
			List<ParticipantResponseDto> participants, RestaurantResponseDto restaurant) {
		Set<Long> savedParticipantIds = routeMapper.findRouteList(roomId).stream()
				.map(ParticipantRouteQueryDto::getParticipantId)
				.collect(Collectors.toSet());

		for (ParticipantResponseDto participant : participants) {
			if (isSavedRoute(savedParticipantIds, participant.getParticipantId())) {
				continue;
			}

			TmapRouteDto route = findParticipantRoute(participant, restaurant);
			if (route != null) {
				insertRoute(roomId, participant.getParticipantId(), route);
			}
		}
	}

	private boolean isSavedRoute(Set<Long> savedParticipantIds, Long participantId) {
		return savedParticipantIds.contains(participantId);
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

	private List<ParticipantRouteResponseDto> findParticipantRouteList(Long roomId) {
		return routeMapper.findRouteList(roomId).stream()
				.map(route -> new ParticipantRouteResponseDto(
						route.getParticipantId(),
						route.getNickname(),
						route.getTimeMinutes(),
						routeMapper.findRoutePointList(route.getRouteId())))
				.toList();
	}

}

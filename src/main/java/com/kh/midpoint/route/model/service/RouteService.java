package com.kh.midpoint.route.model.service;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.external.tmap.TmapRouteClient;
import com.kh.midpoint.external.tmap.TmapRouteDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.restaurant.model.service.RestaurantService;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.route.model.dto.ParticipantRouteResponseDto;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.service.SelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

	private final RoomService roomService;
	private final ParticipantService participantService;
	private final SelectionService selectionService;
	private final RestaurantService restaurantService;
	private final TmapRouteClient tmapRouteClient;

	@Transactional(readOnly = true)
	public RouteResponseDto findRoute(String roomUuid) {
		roomService.findRoom(roomUuid);

		List<ParticipantResponseDto> participants = participantService.findAllParticipants(roomUuid);
		List<Long> restaurantIds = participants.stream()
				.map(participant -> selectionService.findSelection(participant.getParticipantId()))
				.filter(Objects::nonNull)
				.map(SelectionResponseDto::getRestaurantId)
				.distinct()
				.toList();

		if (restaurantIds.isEmpty()) {
			throw new InvalidStateException("식당 선택을 완료한 참가자가 없습니다.");
		}

		Long restaurantId = restaurantIds.get(ThreadLocalRandom.current().nextInt(restaurantIds.size()));
		RestaurantResponseDto restaurant = restaurantService.findRestaurantList(roomUuid).stream()
				.filter(item -> restaurantId.equals(item.getRestaurantId()))
				.findFirst()
				.orElseThrow(() -> new NotFoundException("선정된 식당을 찾을 수 없습니다: " + restaurantId));

		List<ParticipantRouteResponseDto> participantRoutes = new ArrayList<>();
		for (ParticipantResponseDto participant : participants) {
			try {
				TmapRouteDto route = tmapRouteClient.getPedestrianRoute(
						participant.getPrefLng(),
						participant.getPrefLat(),
						restaurant.getLng(),
						restaurant.getLat()
				);
				participantRoutes.add(new ParticipantRouteResponseDto(
						participant.getParticipantId(),
						participant.getNickname(),
						route.getTimeMinutes(),
						route.getPoints()
				));
			} catch (RuntimeException e) {
				log.warn("참가자 {} 도보 경로 조회 실패", participant.getParticipantId(), e);
			}
		}

		return new RouteResponseDto(restaurant, participantRoutes);
	}

}

package com.kh.midpoint.route.model.service;

import com.kh.midpoint.room.model.vo.RoomStage;
import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.roomresult.model.service.RoomResultService;
import com.kh.midpoint.route.model.dao.RouteMapper;
import com.kh.midpoint.route.model.dto.ParticipantRouteQueryDto;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
	private final RouteComposer routeComposer;
	private final RoomResultResolver roomResultResolver;

	// 참가자별 외부 경로 API 호출이 끝날 때까지 DB 트랜잭션이 유지되지 않도록
	// 외부 API 조회는 트랜잭션 밖에서 수행한다.
	// 경로와 좌표 저장은 참가자별 이동수단 하나의 단위로 짧게 처리한다.
	public RouteResponseDto insertRouteResult(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		roomResultResolver.validateGameNotPlaying(room.getStage());

		List<ParticipantResponseDto> participants = participantService.findParticipantList(roomUuid);
		RestaurantResponseDto restaurant = roomResultResolver.insertRoomResult(roomUuid, room.getRoomId());

		List<ParticipantRouteQueryDto> routes = routeComposer.insertMissingRouteList(room.getRoomId(), participants, restaurant);
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

	private void validateTravelMode(String travelMode) {
		if (!walkMode.equals(travelMode) && !transitMode.equals(travelMode)) {
			throw new InvalidStateException("이동수단은 WALK 또는 TRANSIT이어야 합니다.");
		}
	}


}

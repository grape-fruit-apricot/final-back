package com.kh.midpoint.point.model.service;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.kh.midpoint.participant.model.dao.ParticipantMapper;
import com.kh.midpoint.restaurant.model.dao.RestaurantMapper;
import com.kh.midpoint.restaurant.model.service.RestaurantService;
import com.kh.midpoint.restaurant.model.vo.Restaurant;
import com.kh.midpoint.selection.model.dao.SelectionMapper;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MidPointService {

	@Value("${midpoint.reset-distance-meters}")
	private double resetDistanceMeters;
	@Value("${route.earth-radius-meters}")
	private double earthRadiusMeters;
	@Value("${room.stage.midpoint-found}")
	private String midpointFoundStage;

	private final MidPointFinder midpointFinder;
	private final ParticipantService participantService;
	private final RoomService roomService;
	private final RestaurantService restaurantService;
	private final RestaurantMapper restaurantMapper;
	private final SelectionMapper selectionMapper;
	private final ParticipantMapper participantMapper;
	private final TransactionTemplate transactionTemplate;

	// 외부 API는 트랜잭션 밖에서 호출한다. 실패하면 기존 데이터는 전혀 변경하지 않는다.
	public NearbyStationDto resetMidpoint(String roomUuid, Long participantId) {
		participantService.validateHost(roomUuid, participantId);
		RoomResponseDto original = roomService.findRoom(roomUuid);
		validateResettable(original);
		NearbyStationDto midpoint = midpointFinder.findMidPoint(
				participantService.findParticipantList(roomUuid));
		boolean replaceRestaurants = findDistanceMeters(original.getMidpointLat(), original.getMidpointLng(),
				midpoint.getLat(), midpoint.getLng()) > resetDistanceMeters;
		List<Restaurant> restaurants = replaceRestaurants
				? restaurantService.findNearbyRestaurantList(roomUuid, midpoint.getLat(), midpoint.getLng())
					.stream().map(nearby -> restaurantService.toApiRestaurant(roomUuid, nearby)).toList()
				: List.of();

		transactionTemplate.executeWithoutResult(status -> {
			RoomResponseDto current = roomService.findRoomForUpdate(roomUuid);
			participantService.validateHost(roomUuid, participantId);
			validateResettable(current);
			// 계산 중 다른 재설정이 위치를 변경했다면 이전 좌표 기준의 결과를 저장하지 않는다.
			if (!Objects.equals(original.getMidpointLat(), current.getMidpointLat())
					|| !Objects.equals(original.getMidpointLng(), current.getMidpointLng())) {
				throw new InvalidStateException("중간 위치가 변경되었습니다. 다시 시도해주세요.");
			}
			Long roomId = current.getRoomId();
			selectionMapper.deleteSelection(roomId);
			participantMapper.resetReady(roomId);
			if (replaceRestaurants) {
				restaurantMapper.deleteRestaurant(roomId);
				if (!restaurants.isEmpty()) {
					restaurantMapper.insertRestaurantList(restaurants);
				}
			}
			String source = midpoint.getName().equals(midpointFinder.getCenterName()) ? "FALLBACK" : "KAKAO";
			roomService.updateMidpoint(roomId, midpoint.getLat(), midpoint.getLng(), source);
		});
		return midpoint;
	}

	private void validateResettable(RoomResponseDto room) {
		if (!midpointFoundStage.equals(room.getStage())
				|| room.getMidpointLat() == null || room.getMidpointLng() == null) {
			throw new InvalidStateException("중간 위치 재설정은 중간지점 확정 후 투표 시작 전에만 가능합니다.");
		}
	}

	// 위경도의 직선거리를 미터로 변환한다(Haversine).
	private double findDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
		double latSin = Math.sin(Math.toRadians(lat2 - lat1) / 2);
		double lngSin = Math.sin(Math.toRadians(lng2 - lng1) / 2);
		double a = latSin * latSin
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * lngSin * lngSin;
		return earthRadiusMeters * 2 * Math.asin(Math.sqrt(Math.min(1.0, Math.max(0.0, a))));
	}

	// 여기에는 @Transactional 을 붙이지 않는다. 중간지점 계산은 카카오 1회 + Tmap 을
	// (후보 수 x 참가자 수)만큼 호출하므로, 트랜잭션 안에서 돌리면 그 시간 내내 DB 커넥션을
	// 붙잡아 관계없는 요청까지 커넥션 대기로 죽는다. 저장은 아래 두 updateXxx 가 각자
	// 트랜잭션을 열어 처리한다.
	public NearbyStationDto findMidpoint(String roomUuid, Long participantId) {
		participantService.validateHost(roomUuid, participantId);

		RoomResponseDto room = roomService.findRoom(roomUuid);
		validateMidpointNotFound(room);

		List<ParticipantResponseDto> participants = participantService.findParticipantList(roomUuid);

		NearbyStationDto midpoint = midpointFinder.findMidPoint(participants);

		String source = midpoint.getName().equals(midpointFinder.getCenterName()) ? "FALLBACK" : "KAKAO";
		roomService.updateMidpoint(room.getRoomId(), midpoint.getLat(), midpoint.getLng(), source);
		roomService.updateStage(room.getRoomId(), midpointFoundStage);

		return midpoint;
	}

	// stage 값을 열거하는 대신 좌표 유무로 판단한다. 좌표가 먼저 저장되므로 이 검사만으로
	// 재실행을 막을 수 있고, 이후 MODE_SELECTED 같은 단계가 생겨도 영향을 받지 않는다.
	private void validateMidpointNotFound(RoomResponseDto room) {
		if (room.getMidpointLat() != null) {
			throw new InvalidStateException("이미 중간지점을 찾은 방입니다.");
		}
	}

}

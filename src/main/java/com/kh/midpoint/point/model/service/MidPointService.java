package com.kh.midpoint.point.model.service;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.kh.midpoint.room.model.vo.RoomStage;
import com.kh.midpoint.common.util.DistanceCalculator;
import com.kh.midpoint.restaurant.model.service.RestaurantService;
import com.kh.midpoint.restaurant.model.vo.Restaurant;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.selection.model.service.SelectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MidPointService {

	@Value("${midpoint.reset-distance-meters}")
	private double resetDistanceMeters;

	private final MidPointFinder midpointFinder;
	private final DistanceCalculator distanceCalculator;
	private final ParticipantService participantService;
	private final RoomService roomService;
	private final RestaurantService restaurantService;
	private final SelectionService selectionService;
	private final TransactionTemplate transactionTemplate;

	// 외부 API는 트랜잭션 밖에서 호출한다. 실패하면 기존 데이터는 전혀 변경하지 않는다.
	// 계산과 반영을 나눠 어느 문장이 트랜잭션 안에 있는지 읽어서 추적하지 않아도 되게 한다.
	public NearbyStationDto resetMidpoint(String roomUuid, Long participantId) {
		participantService.validateHost(roomUuid, participantId);
		RoomResponseDto original = roomService.findRoom(roomUuid);
		validateResettable(original);

		ResetCandidate candidate = findResetCandidate(roomUuid, original);
		updateByResetCandidate(roomUuid, participantId, original, candidate);

		return candidate.midpoint();
	}

	// 트랜잭션 밖에서 계산한다. 카카오와 Tmap 을 여러 번 부르므로 이 사이에 DB 커넥션을
	// 붙잡고 있으면 안 된다.
	private ResetCandidate findResetCandidate(String roomUuid, RoomResponseDto original) {
		NearbyStationDto midpoint = midpointFinder.findMidPoint(
				participantService.findParticipantList(roomUuid));

		// 중간지점이 조금만 움직였으면 기존 식당 목록을 그대로 쓴다. 멀리 옮겨갔을 때만 다시 받는다.
		boolean replaceRestaurants = distanceCalculator.findDistanceMeters(original.getMidpointLat(), original.getMidpointLng(),
				midpoint.getLat(), midpoint.getLng()) > resetDistanceMeters;
		List<Restaurant> restaurants = replaceRestaurants
				? restaurantService.findNearbyRestaurantList(roomUuid, midpoint.getLat(), midpoint.getLng())
					.stream().map(nearby -> restaurantService.toApiRestaurant(roomUuid, nearby)).toList()
				: List.of();

		return new ResetCandidate(midpoint, replaceRestaurants, restaurants);
	}

	// 트랜잭션 안에서 반영한다. 방을 잠그고 계산의 전제가 그대로인지 다시 확인한 뒤에만 쓴다.
	private void updateByResetCandidate(String roomUuid, Long participantId,
			RoomResponseDto original, ResetCandidate candidate) {
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
			selectionService.deleteSelectionList(roomId);
			participantService.updateReadyReset(roomId);
			if (candidate.replaceRestaurants()) {
				restaurantService.updateRestaurantListByReset(roomId, candidate.restaurants());
			}

			NearbyStationDto midpoint = candidate.midpoint();
			String source = midpoint.getName().equals(midpointFinder.getCenterName()) ? "FALLBACK" : "KAKAO";
			roomService.updateMidpoint(roomId, midpoint.getLat(), midpoint.getLng(), source);
		});
	}

	// 트랜잭션 밖에서 구한 값을 한 덩어리로 옮긴다.
	private record ResetCandidate(NearbyStationDto midpoint, boolean replaceRestaurants,
			List<Restaurant> restaurants) {
	}

	private void validateResettable(RoomResponseDto room) {
		if (!RoomStage.MIDPOINT_FOUND.is(room.getStage())
				|| room.getMidpointLat() == null || room.getMidpointLng() == null) {
			throw new InvalidStateException("중간 위치 재설정은 중간지점 확정 후 투표 시작 전에만 가능합니다.");
		}
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
		// 좌표와 단계를 한 트랜잭션으로 묶는다. 따로 커밋하면 단계 갱신이 실패했을 때
		// 좌표만 남는데, 재실행 여부를 좌표로 판정하므로 방이 단계가 멈춘 채 갇힌다.
		transactionTemplate.executeWithoutResult(status -> {
			roomService.updateMidpoint(room.getRoomId(), midpoint.getLat(), midpoint.getLng(), source);
			roomService.updateStage(room.getRoomId(), RoomStage.MIDPOINT_FOUND.name());
		});

		insertNearbyRestaurantList(roomUuid, midpoint);

		return midpoint;
	}

	// 중간지점이 확정된 뒤에 따로 저장한다. 같은 트랜잭션에서 돌리면 카카오 식당 조회가
	// 실패했을 때 확정된 중간지점까지 롤백된다. 식당 목록은 없어도 중간지점은 살아 있어야 하므로
	// 실패를 삼키고 로그만 남긴다.
	private void insertNearbyRestaurantList(String roomUuid, NearbyStationDto midpoint) {
		try {
			restaurantService.insertNearbyRestaurantList(roomUuid, midpoint.getLat(), midpoint.getLng());
		} catch (RuntimeException e) {
			log.warn("주변 식당 저장 실패 - roomUuid={}, {}", roomUuid, e.toString());
		}
	}

	// stage 값을 열거하는 대신 좌표 유무로 판단한다. 좌표가 먼저 저장되므로 이 검사만으로
	// 재실행을 막을 수 있고, 이후 MODE_SELECTED 같은 단계가 생겨도 영향을 받지 않는다.
	private void validateMidpointNotFound(RoomResponseDto room) {
		if (room.getMidpointLat() != null) {
			throw new InvalidStateException("이미 중간지점을 찾은 방입니다.");
		}
	}

}

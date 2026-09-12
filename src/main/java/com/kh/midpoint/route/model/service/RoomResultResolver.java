package com.kh.midpoint.route.model.service;

import com.kh.midpoint.room.model.vo.RoomStage;
import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.restaurant.model.service.RestaurantService;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.roomresult.model.service.RoomResultService;
import com.kh.midpoint.roomresult.model.vo.RoomResult;
import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.service.SelectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// 어느 식당으로 갈지 정하고 못 박는 일만 한다. 경로를 만들거나 응답을 조립하지 않는다.
// 무작위 추첨은 경로와 아무 상관이 없는데도 RouteService 안에 있었다.
@Service
@RequiredArgsConstructor
public class RoomResultResolver {

	private final RoomService roomService;
	private final RoomResultService roomResultService;
	private final RestaurantService restaurantService;
	private final SelectionService selectionService;
	private final TransactionTemplate transactionTemplate;

	// 게임이 도는 중에는 결과를 확정하지 않는다. 이게 없으면 방장이 무작위 우회를 눌러
	// 참가자들이 주머니를 고르는 도중에 결과를 가로챌 수 있다.
	// 게임이 끝나거나 중단되면 RESOLVING 으로 돌아오므로 그때부터 확정할 수 있다.
	public void validateGameNotPlaying(String stage) {
		if (RoomStage.GAME_PLAYING.is(stage)) {
			throw new InvalidStateException("게임이 진행 중입니다.");
		}
	}

	// 결과 확정은 진입점이 셋이다(투표가 RANDOM 으로 끝날 때, 게임이 끝날 때, 방장이 직접 누를 때).
	// 조회와 확정을 한 트랜잭션으로 묶지 않으면 둘이 동시에 들어왔을 때 양쪽 다 "결과 없음" 을 보고
	// 서로 다른 식당을 뽑는다. UK_ROOM_RESULT_ROOM 이 데이터는 지켜주지만 진 쪽이 예외를 던져
	// 결과가 정상 확정된 방에 오류 메시지가 뿌려진다.
	// 방 행을 잠그는 것은 다른 쓰기 경로(투표 집계, 게임 시작)가 이미 쓰는 방식과 같다.
	// 외부 API 호출은 이 블록 밖에 있으므로 잠금 구간은 짧게 유지된다.
	public RestaurantResponseDto insertRoomResult(String roomUuid, Long roomId) {
		return transactionTemplate.execute(status -> {
			RoomResponseDto locked = roomService.findRoomForUpdate(roomUuid);
			// 잠금을 잡기 전 검사와 잠금 사이에 게임이 시작될 수 있어 여기서 다시 본다.
			validateGameNotPlaying(locked.getStage());

			RestaurantResponseDto confirmed = roomResultService.findRoomResult(roomId);
			if (confirmed != null) {
				return confirmed;
			}

			Long restaurantId = findRandomRestaurantId(findSelectedRestaurantIdList(roomUuid));

			RoomResult roomResult = RoomResult.builder()
					.roomId(roomId)
					.restaurantId(restaurantId)
					.build();
			roomResultService.insertRoomResult(roomResult);

			return restaurantService.findRestaurant(restaurantId);
		});
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

}

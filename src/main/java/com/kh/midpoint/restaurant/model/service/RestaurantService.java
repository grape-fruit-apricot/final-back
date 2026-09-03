package com.kh.midpoint.restaurant.model.service;

import com.kh.midpoint.common.exception.DuplicateException;
import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.external.kakao.KakaoLocalClient;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.restaurant.model.dao.RestaurantMapper;
import com.kh.midpoint.restaurant.model.dto.KakaoRestaurantResponseDto;
import com.kh.midpoint.restaurant.model.dto.RestaurantCreateRequestDto;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.restaurant.model.vo.Restaurant;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

	private final RestaurantMapper restaurantMapper;
	private final RoomService roomService;
	private final ParticipantService participantService;
	private final KakaoLocalClient kakaoLocalClient;

	@Transactional
	public void insertRestaurant(String roomUuid, RestaurantCreateRequestDto requestDto) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		if (!"MIDPOINT_FOUND".equals(room.getStage())) {
			throw new InvalidStateException("중간 지점이 결정된 상태에서만 식당을 등록할 수 있습니다.");
		}

		participantService.validateParticipant(roomUuid, requestDto.getParticipantId());

		Restaurant restaurant = Restaurant.builder()
			.roomUuid(roomUuid)
			.participantId(requestDto.getParticipantId())
			.source("MANUAL")
			.kakaoPlaceId(requestDto.getKakaoPlaceId())
			.name(requestDto.getName())
			.category(requestDto.getCategory())
			.address(requestDto.getAddress())
			.roadAddress(requestDto.getRoadAddress())
			.phone(requestDto.getPhone())
			.placeUrl(requestDto.getPlaceUrl())
			.lat(requestDto.getLat())
			.lng(requestDto.getLng())
			.build();

		// 방마다 같은 카카오 장소는 1건만 저장된다(UK_RESTAURANT_ROOM_KAKAO).
		// 이미 목록에 있는 식당을 또 추가하면 여기서 걸린다.
		try {
			restaurantMapper.insertRestaurantList(List.of(restaurant));
		} catch (DuplicateKeyException e) {
			throw new DuplicateException("이미 목록에 있는 식당입니다: " + requestDto.getName());
		}
	}

	// 중간지점이 확정된 직후, 그 좌표 주변 식당을 카카오에서 받아 방의 식당 목록으로 저장한다.
	// 좌표를 인자로 받는 이유는 호출 시점에 이미 알고 있는 값을 다시 조회하지 않기 위해서다.
	@Transactional
	public void insertNearbyRestaurantList(String roomUuid, Double lat, Double lng) {
		List<KakaoRestaurantResponseDto> nearbyList = kakaoLocalClient.findNearbyRestaurantList(lat, lng);
		if (nearbyList.isEmpty()) {
			return;
		}

		List<Restaurant> restaurants = nearbyList.stream()
				.map(nearby -> toApiRestaurant(roomUuid, nearby))
				.toList();

		restaurantMapper.insertRestaurantList(restaurants);
	}

	// 자동 수집이라 등록자(ADDED_BY)가 없다. 카카오 장소 ID 는 숫자 문자열이라 NUMBER 컬럼에 맞춰 변환한다.
	private Restaurant toApiRestaurant(String roomUuid, KakaoRestaurantResponseDto nearby) {
		return Restaurant.builder()
				.roomUuid(roomUuid)
				.source("API")
				.kakaoPlaceId(Long.valueOf(nearby.getKakaoPlaceId()))
				.name(nearby.getName())
				.category(nearby.getCategory())
				.address(nearby.getAddress())
				.roadAddress(toRoadAddress(nearby))
				.phone(nearby.getPhone())
				.placeUrl(nearby.getPlaceUrl())
				.lat(nearby.getLat())
				.lng(nearby.getLng())
				.build();
	}

	// 도로명주소가 없는 장소가 실제로 있는데, Oracle 은 빈 문자열을 NULL 로 저장하므로
	// 그대로 넣으면 NOT NULL 인 ROAD_ADDRESS 때문에 목록 전체 저장이 실패한다. 지번주소로 대체한다.
	private String toRoadAddress(KakaoRestaurantResponseDto nearby) {
		String roadAddress = nearby.getRoadAddress();
		return (roadAddress == null || roadAddress.isBlank()) ? nearby.getAddress() : roadAddress;
	}

	@Transactional(readOnly = true)
	public List<RestaurantResponseDto> findRestaurantList(String roomUuid) {
		roomService.findRoom(roomUuid);
		return restaurantMapper.findRestaurantList(roomUuid);
	}

	@Transactional(readOnly = true)
	public List<KakaoRestaurantResponseDto> findNearbyRestaurantList(String roomUuid, Double lat, Double lng) {
		roomService.findRoom(roomUuid);
		return kakaoLocalClient.findNearbyRestaurantList(lat, lng);
	}

}

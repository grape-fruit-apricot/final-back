package com.kh.midpoint.room.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.room.model.dao.RoomMapper;
import com.kh.midpoint.room.model.dto.RoomCreateRequestDto;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.vo.Room;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

	private final RoomMapper roomMapper;

	@Transactional
	public RoomResponseDto insertRoom(RoomCreateRequestDto requestDto) {
		Room room = Room.create(requestDto.getMaxParticipants());
		roomMapper.insertRoom(room);
		return roomMapper.findRoom(room.getRoomUuid());
	}

	@Transactional(readOnly = true)
	public RoomResponseDto findRoom(String roomUuid) {
		RoomResponseDto responseDto = roomMapper.findRoom(roomUuid);
		if (responseDto == null) {
			throw new NotFoundException("존재하지 않는 방입니다: " + roomUuid);
		}
		return responseDto;
	}

	// 같은 방을 동시에 바꾸는 작업(투표 집계, 게임 시작)을 직렬화하기 위해 방 행을 잠근다.
	// 잠금은 트랜잭션이 끝날 때 풀리므로 호출하는 쪽이 쓰기 트랜잭션이어야 한다.
	@Transactional
	public RoomResponseDto findRoomForUpdate(String roomUuid) {
		RoomResponseDto responseDto = roomMapper.findRoomForUpdate(roomUuid);
		if (responseDto == null) {
			throw new NotFoundException("존재하지 않는 방입니다: " + roomUuid);
		}
		return responseDto;
	}

	@Transactional
	public void updateMidpoint(Long roomId, Double lat, Double lng, String source) {
		Room room = Room.builder()
				.roomId(roomId)
				.midpointLat(lat)
				.midpointLng(lng)
				.midpointSource(source)
				.build();
		int updatedRows = roomMapper.updateMidpoint(room);
		validateUpdatedRoom(updatedRows);
	}

	@Transactional
	public void updateStage(Long roomId, String stage) {
		validateStage(stage);
		Room room = Room.builder().roomId(roomId).stage(stage).build();
		int updatedRows = roomMapper.updateStage(room);
		validateUpdatedRoom(updatedRows);
	}

	// 만료된 방을 상한 개수만큼 지운다. 지운 방 수를 돌려준다.
	//
	// 한 트랜잭션으로 묶는 이유는, 중간에 실패했을 때 일부만 지워진 상태로 남지 않게 하기
	// 위해서다. 방 하나가 경로점까지 수천 행이라 상한(cleanup.batch-size)이 이 트랜잭션의
	// 크기를 정한다. 남은 방은 다음 주기가 이어서 가져간다.
	//
	// 지울 방이 없으면 DELETE 를 부르지 않는다. IN () 은 빈 목록으로 만들 수 없다.
	@Transactional
	public int deleteExpiredRoomList(int batchSize) {
		List<Long> expiredRoomIds = roomMapper.findExpiredRoomIdList(batchSize);
		if (expiredRoomIds.isEmpty()) {
			return 0;
		}
		return roomMapper.deleteRoomList(expiredRoomIds);
	}

	private void validateStage(String stage) {
		if (!"WAITING".equals(stage)
				&& !"MODE_SELECTED".equals(stage)
				&& !"MIDPOINT_FOUND".equals(stage)
				&& !"RESOLVING".equals(stage)
				&& !"GAME_PLAYING".equals(stage)
				&& !"RESOLVED".equals(stage)) {
			throw new InvalidStateException("올바르지 않은 방 상태입니다: " + stage);
		}
	}

	private void validateUpdatedRoom(int updatedRows) {
		if (updatedRows == 0) {
			throw new NotFoundException("수정할 방을 찾을 수 없습니다.");
		}
	}

}

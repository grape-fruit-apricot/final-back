package com.kh.midpoint.participant.model.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.ForbiddenException;
import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.participant.model.dao.ParticipantMapper;
import com.kh.midpoint.participant.model.dto.JoinRoomRequestDto;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.vo.Participant;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipantService {
	
	private final ParticipantMapper participantMapper;
	private final RoomService roomService;
	
	@Transactional
	public ParticipantResponseDto insertParticipant(String roomUuid, JoinRoomRequestDto request) {
		// 시작 처리와 같은 방 행을 잠가 동시 입장의 정원 초과와 시작 이후 입장을 막는다.
		RoomResponseDto room = roomService.findRoomForUpdate(roomUuid);
		validateParticipantAdmission(room);

		Participant participant = Participant.builder()
				.roomId(room.getRoomId())
				.nickname(request.getNickname())
				.prefLat(request.getLat())
				.prefLng(request.getLng())
				.joinedAt(LocalDateTime.now())
				.build();

		participantMapper.insertParticipant(participant);

		return participantMapper.findParticipant(participant.getParticipantId());
	}

	private void validateParticipantAdmission(RoomResponseDto room) {
		// 준비 여부와 관계없이 방장이 진행 방식 투표를 시작하기 전까지만 입장할 수 있다.
		if (!"WAITING".equals(room.getStage()) && !"MIDPOINT_FOUND".equals(room.getStage())) {
			throw new InvalidStateException("이미 시작된 방이라 입장할 수 없습니다.");
		}
		if (participantMapper.findParticipantList(room.getRoomId()).size() >= room.getMaxParticipants()) {
			throw new InvalidStateException("방 인원이 가득 찼습니다.");
		}
	}

	@Transactional
	public void deleteParticipant(String roomUuid, Long participantId) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		ParticipantResponseDto participant = findParticipantInRoom(room, participantId);
		validateNotPlayingGame(room);

		participantMapper.deleteParticipant(participantId);

		if (isHost(participant)) {
			participantMapper.updateNextHost(participant.getRoomId());
		}
	}

	// 게임 시작 전 준비 상태를 뒤집는다. 준비한 사람이 같은 버튼을 다시 누르면 준비가 풀린다.
	@Transactional
	public void updateReady(String roomUuid, Long participantId) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		ParticipantResponseDto participant = findParticipantInRoom(room, participantId);
		validateReadyChangeable(room);

		Participant updated = Participant.builder()
				.participantId(participant.getParticipantId())
				.isReady("Y".equals(participant.getIsReady()) ? "N" : "Y")
				.build();

		participantMapper.updateReady(updated);
	}

	// 입장 가능 단계와 같은 기준이다. 방장이 시작을 누르기 전까지만 준비를 바꿀 수 있다.
	// 게임 인원은 방장 + 준비 완료로 정해지므로(GameService.findPlayerList),
	// 시작 이후에 준비를 풀 수 있으면 이미 순번까지 짜인 게임에서 빠져나가게 된다.
	private void validateReadyChangeable(RoomResponseDto room) {
		String stage = room.getStage();
		if (!"WAITING".equals(stage) && !"MIDPOINT_FOUND".equals(stage)) {
			throw new InvalidStateException("이미 시작된 방이라 준비 상태를 바꿀 수 없습니다.");
		}
	}

	@Transactional(readOnly = true)
	public void validateHost(String roomUuid, Long participantId) {
		ParticipantResponseDto participant = findParticipantInRoom(roomService.findRoom(roomUuid), participantId);
		if (!isHost(participant)) {
			throw new ForbiddenException("방장만 수행할 수 있는 작업입니다.");
		}
	}

	// 클라이언트가 보낸 participantId 가 정말 그 방의 참가자인지 확인한다.
	// 이게 없으면 A방 uuid 와 B방 participantId 를 섞어 남의 방 참가자 이름으로 요청할 수 있다.
	@Transactional(readOnly = true)
	public void validateParticipant(String roomUuid, Long participantId) {
		findParticipantInRoom(roomService.findRoom(roomUuid), participantId);
	}

	// 방은 호출하는 쪽에서 한 번만 읽어 넘긴다. 여기서 다시 읽으면 한 트랜잭션 안에서
	// 같은 방 행을 두 번 조회하게 되고, 그 사이에 단계가 바뀌면 두 검사가 서로 다른 방을 보게 된다.
	private ParticipantResponseDto findParticipantInRoom(RoomResponseDto room, Long participantId) {
		ParticipantResponseDto participant = participantMapper.findParticipant(participantId);
		if (participant == null || !participant.getRoomId().equals(room.getRoomId())) {
			throw new NotFoundException("존재하지 않는 참가자입니다: " + participantId);
		}
		return participant;
	}

	// 게임 중에는 참가자 행을 지우지 않는다. FK 가 전부 ON DELETE CASCADE 라
	// 지우는 순간 GAME_PARTICIPANT·GAME_PICK·SELECTION 까지 함께 사라져 게임이 깨진다.
	// 게임 중 이탈은 GAME_PARTICIPANT.LEFT_AT 을 남기는 소켓 경로(/app/game/leave)로 처리한다.
	private void validateNotPlayingGame(RoomResponseDto room) {
		if ("GAME_PLAYING".equals(room.getStage())) {
			throw new InvalidStateException("게임이 진행 중이라 나갈 수 없습니다.");
		}
	}

	private boolean isHost(ParticipantResponseDto participant) {
		return "Y".equals(participant.getIsHost());
	}

	@Transactional(readOnly = true)
	public List<ParticipantResponseDto> findParticipantList(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);

		return participantMapper.findParticipantList(room.getRoomId());
	}

}

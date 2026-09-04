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
		RoomResponseDto room = roomService.findRoom(roomUuid);

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

	@Transactional
	public void deleteParticipant(String roomUuid, Long participantId) {
		ParticipantResponseDto participant = findParticipantInRoom(roomUuid, participantId);
		validateNotPlayingGame(roomUuid);

		participantMapper.deleteParticipant(participantId);

		if (isHost(participant)) {
			participantMapper.updateNextHost(participant.getRoomId());
		}
	}

	// 게임 시작 전 준비 완료 표시. 피그마상 준비를 해제하는 동작은 없어 'Y' 로만 바꾼다.
	@Transactional
	public void updateReady(String roomUuid, Long participantId) {
		ParticipantResponseDto participant = findParticipantInRoom(roomUuid, participantId);

		Participant updated = Participant.builder()
				.participantId(participant.getParticipantId())
				.isReady("Y")
				.build();

		participantMapper.updateReady(updated);
	}

	@Transactional(readOnly = true)
	public void validateHost(String roomUuid, Long participantId) {
		ParticipantResponseDto participant = findParticipantInRoom(roomUuid, participantId);
		if (!isHost(participant)) {
			throw new ForbiddenException("방장만 수행할 수 있는 작업입니다.");
		}
	}

	// 클라이언트가 보낸 participantId 가 정말 그 방의 참가자인지 확인한다.
	// 이게 없으면 A방 uuid 와 B방 participantId 를 섞어 남의 방 참가자 이름으로 요청할 수 있다.
	@Transactional(readOnly = true)
	public void validateParticipant(String roomUuid, Long participantId) {
		findParticipantInRoom(roomUuid, participantId);
	}

	private ParticipantResponseDto findParticipantInRoom(String roomUuid, Long participantId) {
		RoomResponseDto room = roomService.findRoom(roomUuid);

		ParticipantResponseDto participant = participantMapper.findParticipant(participantId);
		if (participant == null || !participant.getRoomId().equals(room.getRoomId())) {
			throw new NotFoundException("존재하지 않는 참가자입니다: " + participantId);
		}
		return participant;
	}

	// 게임 중에는 참가자 행을 지우지 않는다. FK 가 전부 ON DELETE CASCADE 라
	// 지우는 순간 GAME_PARTICIPANT·GAME_PICK·SELECTION 까지 함께 사라져 게임이 깨진다.
	// 게임 중 이탈은 GAME_PARTICIPANT.LEFT_AT 을 남기는 소켓 경로(/app/game/leave)로 처리한다.
	private void validateNotPlayingGame(String roomUuid) {
		if ("GAME_PLAYING".equals(roomService.findRoom(roomUuid).getStage())) {
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

package com.kh.midpoint.participant.model.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kh.midpoint.common.exception.ForbiddenException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.participant.model.dao.ParticipantMapper;
import com.kh.midpoint.participant.model.dto.JoinRoomRequest;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.vo.Participant;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipantService {
	
	private final ParticipantMapper participantMapper;
	private final RoomService roomService;
	
	@Transactional
	public ParticipantResponseDto join(String roomUuid, JoinRoomRequest request) {
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

	public void validateHost(String roomUuid, Long participantId) {
		ParticipantResponseDto participant = findParticipantInRoom(roomUuid, participantId);
		if (!isHost(participant)) {
			throw new ForbiddenException("방장만 수행할 수 있는 작업입니다.");
		}
	}

	private ParticipantResponseDto findParticipantInRoom(String roomUuid, Long participantId) {
		RoomResponseDto room = roomService.findRoom(roomUuid);

		ParticipantResponseDto participant = participantMapper.findParticipant(participantId);
		if (participant == null || !participant.getRoomId().equals(room.getRoomId())) {
			throw new NotFoundException("존재하지 않는 참가자입니다: " + participantId);
		}
		return participant;
	}

	private boolean isHost(ParticipantResponseDto participant) {
		return "Y".equals(participant.getIsHost());
	}

	public List<ParticipantResponseDto> findAllParticipants(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		
		List<ParticipantResponseDto> participants = participantMapper.findAllParticipants(room.getRoomId());
		
		return participants;
	}

}

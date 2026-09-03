package com.kh.midpoint.vote.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.vote.model.dao.ModeVoteMapper;
import com.kh.midpoint.vote.model.dto.ModeVoteRequestDto;
import com.kh.midpoint.vote.model.dto.ModeVoteResponseDto;
import com.kh.midpoint.vote.model.dto.ModeVoteStatusDto;
import com.kh.midpoint.vote.model.vo.ModeVote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 게임으로 정할지 무작위로 정할지 참가자들이 투표한다.
// 전원이 투표를 마치면 다수결로 정하고, 동점이면 방장이 고른 쪽으로 한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeVoteService {

	public static final String MODE_GAME = "GAME";
	public static final String MODE_RANDOM = "RANDOM";

	private final ModeVoteMapper modeVoteMapper;
	private final RoomService roomService;
	private final ParticipantService participantService;

	// 방장이 투표를 연다. 다시 열면 이전 표를 지우고 처음부터 받는다.
	@Transactional
	public ModeVoteStatusDto startModeVote(String roomUuid, Long participantId) {
		participantService.validateHost(roomUuid, participantId);

		RoomResponseDto room = roomService.findRoom(roomUuid);
		validateMidpointFound(room);

		modeVoteMapper.deleteModeVoteList(roomUuid);
		roomService.updateStage(room.getRoomId(), "MODE_SELECTED");

		return findModeVoteStatus(roomUuid);
	}

	@Transactional
	public ModeVoteStatusDto insertModeVote(String roomUuid, Long participantId, ModeVoteRequestDto requestDto) {
		participantService.validateParticipant(roomUuid, participantId);

		RoomResponseDto room = roomService.findRoom(roomUuid);
		validateVoteOpen(room);
		validateVoteMode(requestDto.getVoteMode());

		ModeVote modeVote = ModeVote.builder()
				.participantId(participantId)
				.voteMode(requestDto.getVoteMode())
				.build();
		modeVoteMapper.insertModeVote(modeVote);

		ModeVoteStatusDto status = findModeVoteStatus(roomUuid);

		// 게임은 아직 없어서 결과를 만들 수 없다. RESOLVING 으로 두고 게임이 붙을 자리를 남긴다.
		// 무작위는 이어서 경로 확정(RouteService)이 돌면서 RESOLVED 로 바꾼다.
		if (MODE_GAME.equals(status.getDecidedMode())) {
			roomService.updateStage(room.getRoomId(), "RESOLVING");
		}

		return status;
	}

	@Transactional(readOnly = true)
	public ModeVoteStatusDto findModeVoteStatus(String roomUuid) {
		List<ParticipantResponseDto> participants = participantService.findParticipantList(roomUuid);
		List<ModeVoteResponseDto> votes = modeVoteMapper.findModeVoteList(roomUuid);

		return new ModeVoteStatusDto(votes, participants.size(), decideMode(participants, votes));
	}

	// 전원이 투표하기 전에는 결정하지 않는다(null).
	private String decideMode(List<ParticipantResponseDto> participants, List<ModeVoteResponseDto> votes) {
		if (participants.isEmpty() || votes.size() < participants.size()) {
			return null;
		}

		long gameCount = votes.stream().filter(vote -> MODE_GAME.equals(vote.getVoteMode())).count();
		long randomCount = votes.size() - gameCount;

		if (gameCount != randomCount) {
			return gameCount > randomCount ? MODE_GAME : MODE_RANDOM;
		}

		return findHostVoteMode(participants, votes);
	}

	// 동점이면 방장이 고른 쪽으로 한다.
	private String findHostVoteMode(List<ParticipantResponseDto> participants, List<ModeVoteResponseDto> votes) {
		Long hostId = participants.stream()
				.filter(participant -> "Y".equals(participant.getIsHost()))
				.map(ParticipantResponseDto::getParticipantId)
				.findFirst()
				.orElse(null);

		return votes.stream()
				.filter(vote -> vote.getParticipantId().equals(hostId))
				.map(ModeVoteResponseDto::getVoteMode)
				.findFirst()
				.orElseGet(() -> {
					// 투표 도중 방장이 나가면 표가 사라질 수 있다. 방을 막아두는 것보다
					// 이미 구현된 무작위로 진행하는 편이 낫다.
					log.warn("동점인데 방장 표를 찾지 못해 무작위로 진행합니다 - hostId={}", hostId);
					return MODE_RANDOM;
				});
	}

	private void validateMidpointFound(RoomResponseDto room) {
		if (!"MIDPOINT_FOUND".equals(room.getStage())) {
			throw new InvalidStateException("중간 지점이 결정된 상태에서만 진행 방식을 정할 수 있습니다.");
		}
	}

	private void validateVoteOpen(RoomResponseDto room) {
		if (!"MODE_SELECTED".equals(room.getStage())) {
			throw new InvalidStateException("투표가 진행 중이 아닙니다.");
		}
	}

	private void validateVoteMode(String voteMode) {
		if (!MODE_GAME.equals(voteMode) && !MODE_RANDOM.equals(voteMode)) {
			throw new InvalidStateException("올바르지 않은 진행 방식입니다: " + voteMode);
		}
	}

}

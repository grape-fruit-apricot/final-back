package com.kh.midpoint.vote.model.service;

import org.springframework.beans.factory.annotation.Value;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.route.model.service.RouteService;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.room.model.vo.RoomStage;
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

	@Value("${vote.mode.game}")
	private String modeGame;
	@Value("${vote.mode.random}")
	private String modeRandom;

	private final ModeVoteMapper modeVoteMapper;
	private final RouteService routeService;
	private final RoomService roomService;
	private final ParticipantService participantService;

	// 방장이 투표를 연다. 다시 열면 이전 표를 지우고 처음부터 받는다.
	@Transactional
	public ModeVoteStatusDto startModeVote(String roomUuid, Long participantId) {
		participantService.validateHost(roomUuid, participantId);

		// 투표를 다시 여는 동안 누가 표를 던지면 지워질 표가 섞인다. 방을 잠그고 처리한다.
		RoomResponseDto room = roomService.findRoomForUpdate(roomUuid);
		validateMidpointFound(room);
		validateAnyReady(roomUuid);

		modeVoteMapper.deleteModeVoteList(roomUuid);
		roomService.updateStage(room.getRoomId(), RoomStage.MODE_SELECTED.name());

		return findModeVoteStatus(roomUuid);
	}

	// "무작위로 결정되면 게임 없이 바로 결과를 확정한다" 는 판단. 전에는 소켓 컨트롤러에 있었다.
	// 확정할 것이 없으면 null 을 돌려준다. 알릴 것이 없다는 뜻이다.
	// 방식을 알리는 일과 나눠 둔 이유는 경로 계산에 몇 초가 걸리기 때문이다.
	// 호출하는 쪽이 방식부터 알린 뒤에 이 메서드를 부른다.
	public RouteResponseDto insertRouteResultIfRandom(String roomUuid, ModeVoteStatusDto status) {
		if (!modeRandom.equals(status.getDecidedMode())) {
			return null;
		}

		return routeService.insertRouteResult(roomUuid);
	}

	@Transactional
	public ModeVoteStatusDto insertModeVote(String roomUuid, Long participantId, ModeVoteRequestDto requestDto) {
		participantService.validateParticipant(roomUuid, participantId);

		// 방을 잠그고 표를 넣는다. 이게 없으면 동시에 던진 표들이 서로의 커밋을 보지 못해
		// 아무도 마지막 한 명이 되지 못하고, 전원이 투표했는데도 방식이 정해지지 않는다.
		RoomResponseDto room = roomService.findRoomForUpdate(roomUuid);
		validateVoteOpen(room);
		validateVoteMode(requestDto.getVoteMode());

		ModeVote modeVote = ModeVote.builder()
				.participantId(participantId)
				.voteMode(requestDto.getVoteMode())
				.build();
		modeVoteMapper.insertModeVote(modeVote);

		ModeVoteStatusDto status = findModeVoteStatus(roomUuid);

		// 게임으로 정해지면 방장이 게임을 시작할 수 있는 상태로 둔다(GameService 가 여기서 이어받는다).
		// 무작위는 이어서 경로 확정(RouteService)이 돌면서 RESOLVED 로 바꾼다.
		if (modeGame.equals(status.getDecidedMode())) {
			roomService.updateStage(room.getRoomId(), RoomStage.RESOLVING.name());
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

		long gameCount = votes.stream().filter(vote -> modeGame.equals(vote.getVoteMode())).count();
		long randomCount = votes.size() - gameCount;

		if (gameCount != randomCount) {
			return gameCount > randomCount ? modeGame : modeRandom;
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
					return modeRandom;
				});
	}

	// 준비를 마친 참가자가 한 명도 없으면 방장이 혼자 진행 방식을 정하게 된다.
	// 방장에게는 준비 버튼 대신 시작 버튼이 있으므로, 여기서 세는 것은 방장을 뺀 나머지다.
	// 이 검사가 게임 최소 인원(방장 + 준비 완료 >= 2)의 전제도 함께 만들어 준다.
	private void validateAnyReady(String roomUuid) {
		boolean anyReady = participantService.findParticipantList(roomUuid).stream()
				.anyMatch(participant -> "Y".equals(participant.getIsReady()));

		if (!anyReady) {
			throw new InvalidStateException("준비를 마친 참가자가 최소 1명 있어야 시작할 수 있습니다.");
		}
	}

	private void validateMidpointFound(RoomResponseDto room) {
		if (!RoomStage.MIDPOINT_FOUND.is(room.getStage())) {
			throw new InvalidStateException("중간 지점이 결정된 상태에서만 진행 방식을 정할 수 있습니다.");
		}
	}

	private void validateVoteOpen(RoomResponseDto room) {
		if (!RoomStage.MODE_SELECTED.is(room.getStage())) {
			throw new InvalidStateException("투표가 진행 중이 아닙니다.");
		}
	}

	private void validateVoteMode(String voteMode) {
		if (!modeGame.equals(voteMode) && !modeRandom.equals(voteMode)) {
			throw new InvalidStateException("올바르지 않은 진행 방식입니다: " + voteMode);
		}
	}

}

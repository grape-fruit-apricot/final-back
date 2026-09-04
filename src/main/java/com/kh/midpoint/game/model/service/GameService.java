package com.kh.midpoint.game.model.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.DuplicateException;
import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.game.model.dao.GameMapper;
import com.kh.midpoint.game.model.dto.GamePickRequestDto;
import com.kh.midpoint.game.model.dto.GamePickResponseDto;
import com.kh.midpoint.game.model.dto.GameQueryDto;
import com.kh.midpoint.game.model.dto.GameStatusDto;
import com.kh.midpoint.game.model.dto.GameWinnerQueryDto;
import com.kh.midpoint.game.model.vo.Game;
import com.kh.midpoint.game.model.vo.GameParticipant;
import com.kh.midpoint.game.model.vo.GamePick;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.roomresult.model.service.RoomResultService;
import com.kh.midpoint.roomresult.model.vo.RoomResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

	public static final String STATUS_PLAYING = "PLAYING";
	public static final String STATUS_FINISHED = "FINISHED";
	public static final String STATUS_ABORTED = "ABORTED";

	private static final String STAGE_RESOLVING = "RESOLVING";
	private static final String STAGE_GAME_PLAYING = "GAME_PLAYING";

	// 잠금 순서는 항상 ROOM -> GAME 으로 고정한다. 게임 진행 중에도 방 상태(stage)를 바꾸므로
	// 순서가 뒤집힌 경로가 하나라도 있으면 투표·게임시작과 맞물려 교착이 날 수 있다.
	private final GameMapper gameMapper;
	private final RoomService roomService;
	private final ParticipantService participantService;
	private final RoomResultService roomResultService;

	// 상수는 전부 application-constant.yml 에 있다. 여기에 기본값을 적지 않는 이유는
	// 출처를 한 곳으로 유지하기 위해서다(키가 빠지면 어떤 키인지 알려주며 기동이 실패한다).
	@Value("${game.bag.base-count}")
	private int bagBaseCount;

	@Value("${game.bag.base-participants}")
	private int bagBaseParticipants;

	@Value("${game.bag.per-participant}")
	private int bagPerParticipant;

	@Value("${game.turn.seconds}")
	private int turnSeconds;

	@Value("${game.min-participants}")
	private int minParticipants;

	@Value("${game.max-consecutive-timeouts}")
	private int maxConsecutiveTimeouts;

	@Transactional
	public GameStatusDto insertGame(String roomUuid, Long participantId) {
		participantService.validateHost(roomUuid, participantId);

		// 방을 잠그고 시작한다. 시작 버튼이 연달아 눌려도 게임이 두 번 만들어지지 않는다
		// (UK_GAME_ROOM 이 막아주긴 하지만, 그 경우 제약 위반 문구가 그대로 나간다).
		RoomResponseDto room = roomService.findRoomForUpdate(roomUuid);
		validateGameStart(room);
		validateRestartable(room.getRoomId());

		List<GameParticipant> players = findPlayerList(roomUuid, room.getRoomId());
		validatePlayerCount(players);

		int bagCount = bagBaseCount + (players.size() - bagBaseParticipants) * bagPerParticipant;

		gameMapper.insertGame(Game.builder()
				.roomId(room.getRoomId())
				.bagCount(bagCount)
				.winningIndex(ThreadLocalRandom.current().nextInt(bagCount))
				.currentParticipantId(players.get(0).getParticipantId())
				.turnSeconds(turnSeconds)
				.build());
		gameMapper.insertGameParticipantList(players);

		roomService.updateStage(room.getRoomId(), STAGE_GAME_PLAYING);

		return findGameStatus(roomUuid);
	}

	@Transactional
	public GameStatusDto insertGamePick(String roomUuid, Long participantId, GamePickRequestDto requestDto) {
		RoomResponseDto room = roomService.findRoomForUpdate(roomUuid);
		GameQueryDto game = findPlayingGameForUpdate(room.getRoomId());

		validateMyTurn(game, participantId);
		validateBagIndex(game, requestDto.getBagIndex());
		validateBagNotOpened(room.getRoomId(), requestDto.getBagIndex());

		gameMapper.insertGamePick(GamePick.builder()
				.roomId(room.getRoomId())
				.participantId(participantId)
				.bagIndex(requestDto.getBagIndex())
				.build());

		// 당첨 위치는 자바로 읽지 않는다. 바뀐 행 수가 1 이면 당첨이다.
		if (gameMapper.updateGameFinished(room.getRoomId(), requestDto.getBagIndex()) == 1) {
			gameMapper.updateGameWinner(participantId);
			// 게임이 끝났으니 결과를 확정할 수 있는 상태로 되돌린다.
			// RouteService 가 이어서 RESOLVED 로 바꾼다.
			roomService.updateStage(room.getRoomId(), STAGE_RESOLVING);
			return findGameStatus(roomUuid);
		}

		updateNextTurn(room.getRoomId(), game, participantId);

		return findGameStatus(roomUuid);
	}

	// 마감이 지난 차례를 다음 사람에게 넘긴다. 누가 몇 번을 불러도 결과가 같다.
	@Transactional
	public GameStatusDto updateTurnExpired(String roomUuid, Integer turnSeq) {
		RoomResponseDto room = roomService.findRoomForUpdate(roomUuid);
		GameQueryDto game = gameMapper.findGameForUpdate(room.getRoomId());
		if (game == null || !STATUS_PLAYING.equals(game.getStatus())) {
			return findGameStatus(roomUuid);
		}

		Long nextParticipantId = findNextParticipantId(room.getRoomId(), game.getCurrentParticipantId());
		if (nextParticipantId == null) {
			updateGameAborted(room.getRoomId());
			return findGameStatus(roomUuid);
		}

		// 조건이 어긋나면 0행이다. 이미 지나간 차례에 대한 뒤늦은 알림이므로 조용히 넘긴다.
		if (gameMapper.updateTurnByTimeout(room.getRoomId(), turnSeq, nextParticipantId, turnSeconds) == 0) {
			return findGameStatus(roomUuid);
		}

		// 아무도 고르지 않으면 주머니가 줄지 않아 게임이 끝나지 않는다. 그때는 중단한다.
		if (game.getConsecutiveTimeouts() + 1 >= maxConsecutiveTimeouts) {
			log.warn("연속 타임아웃 한도를 넘어 게임을 중단합니다. roomUuid={}", roomUuid);
			updateGameAborted(room.getRoomId());
		}

		return findGameStatus(roomUuid);
	}

	// 나가기 버튼으로 게임에서 빠진다. 참가자 행은 지우지 않고 이탈 시각만 남긴다(BR-18).
	@Transactional
	public GameStatusDto updateGameParticipantLeft(String roomUuid, Long participantId) {
		RoomResponseDto room = roomService.findRoomForUpdate(roomUuid);
		GameQueryDto game = gameMapper.findGameForUpdate(room.getRoomId());
		if (game == null || !STATUS_PLAYING.equals(game.getStatus())) {
			return findGameStatus(roomUuid);
		}

		if (gameMapper.updateGameParticipantLeft(participantId) == 0) {
			return findGameStatus(roomUuid);
		}

		if (participantId.equals(game.getCurrentParticipantId())) {
			updateTurnByLeave(room.getRoomId(), participantId);
		}

		return findGameStatus(roomUuid);
	}

	// 연결이 끊긴 사람의 차례를 즉시 넘긴다. 게임에서 빼지는 않는다.
	// 새로고침이나 탭 이동으로도 연결은 끊기므로, 끊겼다고 탈락시키면 안 된다.
	// 아무것도 하지 않았으면 null 을 돌려준다(브로드캐스트할 이유가 없다).
	@Transactional
	public GameStatusDto updateTurnBySkip(String roomUuid, Long participantId) {
		RoomResponseDto room = roomService.findRoom(roomUuid);

		// 이 메서드는 연결이 끊길 때마다 불린다. 채팅 탭을 오가기만 해도 끊기므로
		// 대부분은 게임과 무관하다. 잠그기 전에 값싼 조회로 먼저 걸러낸다.
		GameQueryDto game = gameMapper.findGame(room.getRoomId());
		if (game == null || !STATUS_PLAYING.equals(game.getStatus())
				|| !participantId.equals(game.getCurrentParticipantId())) {
			return null;
		}

		// 여기서부터는 실제로 바꿔야 하므로 ROOM -> GAME 순서로 잠근다.
		roomService.findRoomForUpdate(roomUuid);
		game = gameMapper.findGameForUpdate(room.getRoomId());
		if (game == null || !STATUS_PLAYING.equals(game.getStatus())
				|| !participantId.equals(game.getCurrentParticipantId())) {
			return null;
		}

		updateTurnByLeave(room.getRoomId(), participantId);

		return findGameStatus(roomUuid);
	}

	// 승자가 고른 식당을 결과로 먼저 박아둔다. 그러면 RouteService 가 무작위 추첨을 건너뛰고
	// 이 결과를 그대로 쓴다(findRestaurant 가 기존 결과를 먼저 확인한다).
	@Transactional
	public void insertRoomResult(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);
		if (roomResultService.findRoomResult(room.getRoomId()) != null) {
			return;
		}

		GameWinnerQueryDto winner = gameMapper.findGameWinner(room.getRoomId());
		if (winner == null || winner.getRestaurantId() == null) {
			// 승자가 식당을 고르지 않았으면 결과를 만들지 않는다.
			// 방이 멈추지 않도록 기존 무작위 확정에 맡긴다.
			log.warn("승자의 선택 식당이 없어 무작위 확정에 맡깁니다. roomUuid={}", roomUuid);
			return;
		}

		roomResultService.insertRoomResult(RoomResult.builder()
				.roomId(room.getRoomId())
				.restaurantId(winner.getRestaurantId())
				.gameParticipantId(winner.getGameParticipantId())
				.build());
	}

	@Transactional(readOnly = true)
	public GameStatusDto findGameStatus(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);

		GameStatusDto status = gameMapper.findGameStatus(room.getRoomId());
		if (status == null) {
			throw new NotFoundException("진행 중인 게임이 없습니다.");
		}

		status.setPlayers(gameMapper.findGamePlayerList(room.getRoomId()));
		status.setPicks(gameMapper.findGamePickList(room.getRoomId()));

		return status;
	}

	// 게임 행을 잠근 뒤, 마감이 지난 차례가 있으면 먼저 정리하고 돌려준다.
	// 이렇게 해두면 타이머가 없어도 들어오는 요청이 스스로 시간을 흘려보낸다.
	private GameQueryDto findPlayingGameForUpdate(Long roomId) {
		GameQueryDto game = gameMapper.findGameForUpdate(roomId);
		if (game == null) {
			throw new NotFoundException("진행 중인 게임이 없습니다.");
		}
		if (!STATUS_PLAYING.equals(game.getStatus())) {
			throw new InvalidStateException("이미 끝난 게임입니다.");
		}

		if ("Y".equals(game.getIsExpired())) {
			Long nextParticipantId = findNextParticipantId(roomId, game.getCurrentParticipantId());
			if (nextParticipantId != null
					&& gameMapper.updateTurnByTimeout(roomId, game.getTurnSeq(), nextParticipantId, turnSeconds) > 0) {
				return gameMapper.findGameForUpdate(roomId);
			}
		}

		return game;
	}

	private void updateNextTurn(Long roomId, GameQueryDto game, Long participantId) {
		Long nextParticipantId = findNextParticipantId(roomId, participantId);
		if (nextParticipantId == null) {
			updateGameAborted(roomId);
			return;
		}

		if (gameMapper.updateTurnByPick(roomId, participantId, nextParticipantId, turnSeconds) == 0) {
			throw new InvalidStateException("지금은 차례가 아닙니다.");
		}
	}

	private void updateTurnByLeave(Long roomId, Long participantId) {
		Long nextParticipantId = findNextParticipantId(roomId, participantId);
		if (nextParticipantId == null) {
			updateGameAborted(roomId);
			return;
		}
		gameMapper.updateTurnByLeave(roomId, participantId, nextParticipantId, turnSeconds);
	}

	private void updateGameAborted(Long roomId) {
		if (gameMapper.updateGameAborted(roomId) > 0) {
			// 방을 막아두지 않는다. 방장이 무작위로 넘길 수 있는 상태로 되돌린다.
			roomService.updateStage(roomId, STAGE_RESOLVING);
		}
	}

	// 남은 사람 중 다음 차례를 고른다. 현재 차례인 사람이 이미 나갔으면 목록에 없으므로 맨 앞부터 돈다.
	private Long findNextParticipantId(Long roomId, Long currentParticipantId) {
		List<Long> activeParticipantIds = gameMapper.findActiveParticipantIdList(roomId);
		if (activeParticipantIds.isEmpty()) {
			return null;
		}

		int index = activeParticipantIds.indexOf(currentParticipantId);
		if (index < 0) {
			return activeParticipantIds.get(0);
		}

		return activeParticipantIds.get((index + 1) % activeParticipantIds.size());
	}

	// 방장과 준비를 마친 참가자만 게임에 넣는다.
	// findParticipantList 에는 ORDER BY 가 없어 반환 순서를 믿을 수 없으므로 여기서 직접 정렬한다.
	private List<GameParticipant> findPlayerList(String roomUuid, Long roomId) {
		List<ParticipantResponseDto> candidates = participantService.findParticipantList(roomUuid).stream()
				.filter(participant -> isHost(participant) || "Y".equals(participant.getIsReady()))
				.sorted(Comparator.comparing((ParticipantResponseDto participant) -> !isHost(participant))
						.thenComparing(ParticipantResponseDto::getParticipantId))
				.toList();

		List<GameParticipant> players = new ArrayList<>();
		for (int turnOrder = 0; turnOrder < candidates.size(); turnOrder++) {
			players.add(GameParticipant.builder()
					.roomId(roomId)
					.participantId(candidates.get(turnOrder).getParticipantId())
					.turnOrder(turnOrder)
					.build());
		}

		return players;
	}

	private boolean isHost(ParticipantResponseDto participant) {
		return "Y".equals(participant.getIsHost());
	}

	private void validateGameStart(RoomResponseDto room) {
		if (!STAGE_RESOLVING.equals(room.getStage())) {
			throw new InvalidStateException("게임을 시작할 수 있는 상태가 아닙니다.");
		}
	}

	// 중단된 게임은 지우고 다시 시작할 수 있다. 참가자당 1행(UK_GAME_PARTICIPANT_PART)이라
	// 지우지 않고 다시 넣으면 제약에 걸린다.
	private void validateRestartable(Long roomId) {
		GameQueryDto game = gameMapper.findGame(roomId);
		if (game == null) {
			return;
		}
		if (STATUS_PLAYING.equals(game.getStatus())) {
			throw new DuplicateException("이미 게임이 진행 중입니다.");
		}
		if (STATUS_FINISHED.equals(game.getStatus())) {
			throw new InvalidStateException("이미 끝난 게임입니다.");
		}
		gameMapper.deleteGame(roomId);
	}

	private void validatePlayerCount(List<GameParticipant> players) {
		if (players.size() < minParticipants) {
			throw new InvalidStateException("게임은 최소 " + minParticipants + "명이어야 시작할 수 있습니다.");
		}
	}

	private void validateMyTurn(GameQueryDto game, Long participantId) {
		if (!participantId.equals(game.getCurrentParticipantId())) {
			throw new InvalidStateException("지금은 차례가 아닙니다.");
		}
	}

	private void validateBagIndex(GameQueryDto game, Integer bagIndex) {
		if (bagIndex < 0 || bagIndex >= game.getBagCount()) {
			throw new InvalidStateException("올바르지 않은 주머니입니다.");
		}
	}

	private void validateBagNotOpened(Long roomId, Integer bagIndex) {
		boolean opened = gameMapper.findGamePickList(roomId).stream()
				.map(GamePickResponseDto::getBagIndex)
				.anyMatch(bagIndex::equals);
		if (opened) {
			throw new DuplicateException("이미 열린 주머니입니다.");
		}
	}

}

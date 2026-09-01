package com.kh.midpoint.game.model.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.DuplicateException;
import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.game.model.dao.GameMapper;
import com.kh.midpoint.game.model.dto.GameResponse;
import com.kh.midpoint.game.model.dto.PlayerResponse;
import com.kh.midpoint.game.model.vo.Game;
import com.kh.midpoint.game.model.vo.GamePlayer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
	private static final int MAX_PLAYERS = 10;
	
	private final GameRuntimeStore runtimeStore;
	private final GameMapper gameMapper;
	
	@Transactional
    public GameResponse insertPlayer(Long roomId, String playerName) {
        // 동시 입장 시 정원과 입장 순서가 꼬이지 않도록 방 행을 잠가 조회한다.
        Game game = gameMapper.findGameForUpdate(roomId);
        // 방이 존재하고 아직 게임을 시작하지 않았을 때만 입장을 허용한다.
        if (game == null) throw new NotFoundException("존재하지 않는 방입니다.");
        if (runtimeStore.gameStarted(roomId)) throw new InvalidStateException("이미 게임이 시작되었습니다.");
        // 활성 참가자 수를 기준으로 최대 정원 초과 여부를 검사한다.
        game.setPlayers(gameMapper.findPlayerList(roomId));
        if (game.getPlayers().stream().filter(this::isActive).count() >= MAX_PLAYERS)
            throw new InvalidStateException("방 정원이 가득 찼습니다.");
        // 이름의 공백과 길이를 정리한 뒤 같은 방의 닉네임 중복을 검사한다.
        String name = cleanName(playerName);
        if (gameMapper.existsPlayerName(roomId, name))
            throw new DuplicateException("이 게임에서 이미 사용된 이름입니다. 다른 이름을 입력해 주세요.");
        int order = gameMapper.findNextPlayerOrder(roomId);
        if (order > MAX_PLAYERS) throw new InvalidStateException("이 방은 더 이상 참가자를 추가할 수 없습니다.");
        // 참가자를 저장하고 DB가 생성한 PARTICIPANT_ID를 playerId로 사용한다.
        GamePlayer player = newPlayer(roomId, name, order);
        gameMapper.insertPlayer(player);
        player.setPlayerId(player.getParticipantId().toString());
        // 최신 참가자 목록을 실시간 상태에 반영하고 새 참가자 관점의 방 상태를 반환한다.
        Game updated = findRequiredGame(roomId);
        runtimeStore.participantsChanged(roomId, activeIds(updated));
        return response(updated, player.getParticipantId());
    }

	//게임 참가자 중 현재 활성 상태인 참가자 ID만 조회하는 메서드
    private Set<String> activeIds(Game game) {
        return game.getPlayers().stream().filter(this::isActive).map(GamePlayer::getPlayerId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

	private GamePlayer newPlayer(Long roomId, String playerName, int order) {
		GamePlayer player = new GamePlayer();
        player.setRoomId(roomId);
        player.setPlayerName(playerName);
        player.setPlayerOrder(order);
        player.setPlayerStatus("ACTIVE");
        return player;
	}

	//DB 정보와 서버 메모리 상태를 프론트 응답 객체로 변환하는 메서드
    private GameResponse response(Game game, Long playerId) {
        Long roomId = game.getRoomId();
        GameRuntimeStore.RuntimeState runtime = runtimeStore.get(roomId);
        Set<String> activeIds = activeIds(game);
        List<PlayerResponse> players = game.getPlayers().stream()
                .map(player -> new PlayerResponse(Long.valueOf(player.getPlayerId()), player.getPlayerName(),
                        player.getPlayerId().equals(game.getHostPlayerId()), isActive(player),
                        runtimeStore.isReady(roomId, player.getPlayerId())))
                .toList();
        Long hostId = Long.valueOf(game.getHostPlayerId());
        return new GameResponse(roomId, playerId, hostId, hostId.equals(playerId), players,
                runtime.gameState(), runtime.version(), runtimeStore.isAllReady(roomId, activeIds),
                runtimeStore.delegationSeconds(roomId, activeIds),
                runtimeStore.forceStartEligible(roomId, activeIds),
                runtimeStore.forceStartSeconds(roomId, activeIds));
    }

    //참가자 이름의 공백과 최대 길이를 검증하는 메서드
    private String cleanName(String name) {
        String cleaned = name.trim();
        if (cleaned.length() > 20)
            throw new InvalidStateException("이름은 20자 이하로 입력해 주세요.");
        return cleaned;
    }
	
	//참가자가 현재 게임에 남아 있는 활성 상태인지 확인하는 메서드
    private boolean isActive(GamePlayer player) { return "ACTIVE".equals(player.getPlayerStatus()); }
    
    
    //게임 존재 여부를 확인하고 현재 참가자 목록을 함께 조회하는 메서드
    private Game findRequiredGame(Long roomId) {
        Game game = gameMapper.findGame(roomId);
        if (game == null) throw new NotFoundException("존재하지 않는 방입니다.");
        game.setPlayers(gameMapper.findPlayerList(roomId));
        return game;
    }
}

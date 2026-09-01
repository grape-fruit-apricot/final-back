package com.kh.midpoint.game.model.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class GameRuntimeStore {
	// 모든 참가자가 준비된 뒤 방장 위임까지 기다리는 시간과 한 턴의 제한시간이다.
    private static final long DELEGATION_SECONDS = 30;
    private static final long FORCE_START_SECONDS = 30;
    private static final int MIN_FORCE_START_PLAYERS = 2;
    private static final long TURN_TIMEOUT_MILLIS = 30_000;

	//서버 메모리에 저장된 게임 상태를 실제 참가자 목록과 맞추는 메서드
    public void participantsChanged(Long gameId, Set<String> activePlayerIds) {
        RuntimeState state = get(gameId);
        synchronized (state) {
            state.readyPlayerIds.retainAll(activePlayerIds);
            // 참가자 입장·퇴장으로 방 구성이 바뀌면 활성화된 버튼도 다시 30초를 기다린다.
            state.forceStartResetAt = Instant.now();
            reconcileTreasurePlayers(state, activePlayerIds);
            updateReadyTime(state, activePlayerIds);
            state.version++;
        }
    }

  //모두 준비한 최초 시간을 기록하고 준비가 풀리면 시간을 초기화하는 메서드
    private void updateReadyTime(RuntimeState state, Set<String> activeIds) {
        if (isAllReady(state, activeIds)) {
            if (state.allReadyAt == null) state.allReadyAt = Instant.now();
        } else {
            state.allReadyAt = null;
        }
    }

  //보물게임 상태의 참가자 목록과 현재 차례를 다시 맞추는 메서드
    private void reconcileTreasurePlayers(RuntimeState state, Set<String> activePlayerIds) {
        Object idsValue = state.gameState.get("activePlayerIds");
        if (!(idsValue instanceof List<?> oldIds)) return;
        List<String> previous = oldIds.stream().map(String::valueOf).toList();
        int previousTurn = state.gameState.get("turn") instanceof Number number ? number.intValue() : 0;
        String currentPlayer = previous.isEmpty() ? null
                : previous.get(Math.floorMod(previousTurn, previous.size()));
        List<String> remaining = previous.stream().filter(activePlayerIds::contains).toList();
        state.gameState.put("activePlayerIds", new ArrayList<>(remaining));

        if (remaining.isEmpty()) {
            state.gameState.clear();
            return;
        }
        if (!"PLAYING".equals(String.valueOf(state.gameState.get("phase")))) {
            retainActiveWinners(state, activePlayerIds);
            return;
        }
        if (remaining.size() == 1) {
            state.gameState.put("phase", "FINISHED");
            state.gameState.put("winners", List.of(remaining.getFirst()));
            state.gameState.put("rematchRequired", false);
            state.gameState.remove("turnDeadlineAt");
            return;
        }
        int nextTurn = currentPlayer != null && remaining.contains(currentPlayer)
                ? remaining.indexOf(currentPlayer)
                : Math.floorMod(previousTurn, remaining.size());
        state.gameState.put("turn", nextTurn);
        state.gameState.put("turnDeadlineAt", System.currentTimeMillis() + TURN_TIMEOUT_MILLIS);
    }

    //현재 방에 남아 있는 참가자만 승자 목록에 유지
    private void retainActiveWinners(RuntimeState state, Set<String> activePlayerIds) {
        Object winnersValue = state.gameState.get("winners");
        if (!(winnersValue instanceof List<?> winners)) return;
        List<String> remainingWinners = winners.stream().map(String::valueOf)
                .filter(activePlayerIds::contains).toList();
        state.gameState.put("winners", remainingWinners);
        state.gameState.put("rematchRequired", false);
    }

    //게임 상태가 만들어져 게임이 시작됐는지 확인하는 메서드
    public boolean gameStarted(Long gameId) {
        RuntimeState state = get(gameId);
        synchronized (state) {
            return !state.gameState.isEmpty();
        }
    }
	
	 // key는 게임방 ID이며 서로 다른 방의 상태와 잠금이 분리된다.
    private final Map<Long, RuntimeState> states = new ConcurrentHashMap<>();



	public static class RuntimeState {
        private Map<String, Object> gameState = new LinkedHashMap<>();
        private final Set<String> readyPlayerIds = new HashSet<>();
        private Instant allReadyAt;
        private Instant forceStartResetAt = Instant.now();
        private long version;

        //외부에서 원본 상태를 변경하지 못하게 게임 상태의 복사본을 반환하는 메서드
        public synchronized Map<String, Object> gameState() { return new LinkedHashMap<>(gameState); }

        //현재 게임 상태의 버전을 반환하는 메서드
        public synchronized long version() { return version; }
    }
	
	 //게임 ID로 서버 메모리의 게임 상태를 조회하고 없으면 새로 만드는 메서드
    public RuntimeState get(Long gameId) {
        return states.computeIfAbsent(gameId, ignored -> new RuntimeState());
    }
	
	 //특정 참가자가 준비 상태인지 확인하는 메서드
    public boolean isReady(Long gameId, String playerId) {
        RuntimeState state = get(gameId);
        synchronized (state) {
            return state.readyPlayerIds.contains(playerId);
        }
    }

    //방장 위임까지 남은 시간을 초 단위로 계산하는 메서드
    public long delegationSeconds(Long gameId, Set<String> activePlayerIds) {
        RuntimeState state = get(gameId);
        synchronized (state) {
            // 방장 외에 위임받을 참가자가 없으면 카운트다운을 시작하지 않는다.
            if (activePlayerIds.size() < 2 || !isAllReady(state, activePlayerIds)
                    || state.allReadyAt == null || !state.gameState.isEmpty()) return 0;
            long elapsed = Duration.between(state.allReadyAt, Instant.now()).getSeconds();
            return Math.max(0, DELEGATION_SECONDS - elapsed);
        }
    }

    // 일부만 준비된 방에서 강제 시작 카운트다운을 보여줄 조건인지 확인한다.
    public boolean forceStartEligible(Long gameId, Set<String> activePlayerIds) {
        RuntimeState state = get(gameId);
        synchronized (state) {
            return forceStartEligible(state, activePlayerIds);
        }
    }

    private boolean forceStartEligible(RuntimeState state, Set<String> activeIds) {
        if (activeIds.size() < MIN_FORCE_START_PLAYERS || state.gameState.size() > 0
                || isAllReady(state, activeIds)) return false;
        long readyCount = activeIds.stream().filter(state.readyPlayerIds::contains).count();
        return readyCount >= MIN_FORCE_START_PLAYERS;
    }
    
    // 마지막 참가자 입장 또는 준비 변경 시점부터 강제 시작까지 남은 초를 계산한다.
    public long forceStartSeconds(Long gameId, Set<String> activePlayerIds) {
        RuntimeState state = get(gameId);
        synchronized (state) {
            if (!forceStartEligible(state, activePlayerIds) || state.forceStartResetAt == null) return 0;
            long elapsed = Duration.between(state.forceStartResetAt, Instant.now()).getSeconds();
            return Math.max(0, FORCE_START_SECONDS - elapsed);
        }
    }
	
	//실행 상태에 현재 활성 참가자의 준비 정보가 모두 포함됐는지 확인하는 메서드
    private boolean isAllReady(RuntimeState state, Set<String> activeIds) {
        return !activeIds.isEmpty() && state.readyPlayerIds.containsAll(activeIds);
    }
    
	//현재 활성 참가자가 모두 준비 상태인지 확인하는 메서드
    public boolean isAllReady(Long gameId, Set<String> activePlayerIds) {
        RuntimeState state = get(gameId);
        synchronized (state) {
            return isAllReady(state, activePlayerIds);
        }
    }
}

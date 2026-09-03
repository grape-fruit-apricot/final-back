package com.kh.midpoint.game.model.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class GameRuntimeStore {

	private final Map<Long, RuntimeState> states = new ConcurrentHashMap<>();

	
	public void participantsChanged(Long roomId, Set<String> activeParticipantIds) {
        RuntimeState state = get(roomId);
        synchronized (state) {
            if (state.activeParticipantIds.equals(activeParticipantIds)) {
                return;
            }
            state.activeParticipantIds = new LinkedHashSet<>(activeParticipantIds);
            reconcilePlayers(state, activeParticipantIds);
            state.version++;
        }
    }
	
	public RuntimeState get(Long roomId) {
        return states.computeIfAbsent(roomId, ignored -> new RuntimeState());
    }
	
	private void reconcilePlayers(RuntimeState state, Set<String> activeIds) {
        List<String> previous = stringList(state.gameState.get("activePlayerIds"));
        if (previous.isEmpty()) {
            return;
        }

        int previousTurn = number(state.gameState.get("turn"));
        String currentId = previous.get(Math.floorMod(previousTurn, previous.size()));
        List<String> remaining = previous.stream().filter(activeIds::contains).toList();
        state.gameState.put("activePlayerIds", new ArrayList<>(remaining));

        if (remaining.isEmpty()) {
            state.gameState.clear();
        } else if ("PLAYING".equals(String.valueOf(state.gameState.get("phase")))
                && remaining.size() == 1) {
            finish(state, remaining.getFirst());
        } else {
            int nextTurn = remaining.contains(currentId)
                    ? remaining.indexOf(currentId)
                    : Math.floorMod(previousTurn, remaining.size());
            state.gameState.put("turn", nextTurn);
        }
    }
	
	private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }

	public static class RuntimeState {
        private Map<String, Object> gameState = new LinkedHashMap<>();
        private Set<String> activeParticipantIds = new LinkedHashSet<>();
        private long version;

        public synchronized Map<String, Object> gameState() {
            return new LinkedHashMap<>(gameState);
        }

        public synchronized long version() {
            return version;
        }
    }
	
	private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
	
	 private void finish(RuntimeState state, String winnerId) {
	        state.gameState.put("phase", "FINISHED");
	        state.gameState.put("turn", 0);
	        state.gameState.put("winners", List.of(winnerId));
	        state.gameState.remove("turnDeadlineAt");
	    }
	
}

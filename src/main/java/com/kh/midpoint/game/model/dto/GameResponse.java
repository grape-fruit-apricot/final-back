package com.kh.midpoint.game.model.dto;

import java.util.List;
import java.util.Map;

import lombok.Value;

@Value
public class GameResponse {

    Long roomId;
    Long playerId;
    Long hostId;
    boolean host;
    List<PlayerResponse> players;
    Map<String, Object> gameState;
    long version;
    boolean allReady;
    long hostDelegationSeconds;
    boolean forceStartEligible;
    long forceStartSeconds;
}

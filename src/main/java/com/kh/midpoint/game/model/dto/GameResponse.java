package com.kh.midpoint.game.model.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GameResponse {

    private String roomUuid;
    private Long participantId;
    private Long hostId;
    private boolean host;
    private List<PlayerResponse> players;
    private Map<String, Object> gameState;
    private long version;
    private boolean allReady;
    private long hostDelegationSeconds;
    private boolean forceStartEligible;
    private long forceStartSeconds;
}
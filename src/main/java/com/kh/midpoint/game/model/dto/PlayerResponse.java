package com.kh.midpoint.game.model.dto;

import lombok.Value;

@Value
public class PlayerResponse {

    Long playerId;
    String name;
    boolean host;
    boolean active;
    boolean ready;
}

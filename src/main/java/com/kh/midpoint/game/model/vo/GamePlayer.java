package com.kh.midpoint.game.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GamePlayer {

    private Long participantId;
    private String playerId;
    private Long roomId;
    private String playerName;
    private int playerOrder;
    private String playerStatus;
}

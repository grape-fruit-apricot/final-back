package com.kh.midpoint.game.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 게임방 참가자의 식별자, 이름, 입장 순서와 활성 상태를 표현한다. */
@Getter
@Setter
@NoArgsConstructor
public class GamePlayer {
    private Long participantId;
    private Long gameId;
    private String playerName;
    private int playerOrder;
    private String playerStatus;
}

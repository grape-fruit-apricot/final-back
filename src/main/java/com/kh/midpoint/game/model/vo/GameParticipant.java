package com.kh.midpoint.game.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 게임 참가자별 최종 승패 결과를 저장할 때 사용하는 MyBatis 모델이다. */
@Getter
@Setter
@NoArgsConstructor
public class GameParticipant {

    private Long participantId;
    private String winner;
}

package com.kh.midpoint.game.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** GAME 테이블의 게임방 정보와 조회 시 결합되는 참가자 목록을 표현한다. */
@Getter
@Setter
@NoArgsConstructor
public class Game {
    private Long gameId;
    private String gameUuid;
    private Long hostParticipantId;
    private LocalDateTime createdAt;
    private List<GamePlayer> players = new ArrayList<>();
}

package com.kh.midpoint.game.model.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Game {
	private Long gameId;
    private String gameUuid;
    private String hostPlayerId;
    private LocalDateTime createdAt;
    private List<GamePlayer> players = new ArrayList<>();
}

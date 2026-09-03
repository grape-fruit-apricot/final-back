package com.kh.midpoint.game.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** 게임방 참가자 조회 결과 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GameParticipantQueryResponseDto {
    private Long participantId;
    private String playerName;
    private String playerStatus;
}

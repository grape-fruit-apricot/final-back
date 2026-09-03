package com.kh.midpoint.game.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** 게임방 내부 조회 결과 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GameRoomQueryResponseDto {
    private Long roomId;
    private String roomUuid;
    private Long hostId;
    private LocalDateTime createdAt;
    private List<GameParticipantQueryResponseDto> players = new ArrayList<>();
}

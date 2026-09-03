package com.kh.midpoint.game.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
public class GameDto {
    private Long roomId;
    private String roomUuid;
    private Long hostParticipantId;
    private LocalDateTime createdAt;
    private List<GamePlayerDto> players = new ArrayList<>();
}
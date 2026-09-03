package com.kh.midpoint.game.model.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
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
public class GameStateUpdateRequestDto {
    @NotNull
    private Long participantId;
    private long version;
    @NotNull
    private Map<String, Object> state;
}

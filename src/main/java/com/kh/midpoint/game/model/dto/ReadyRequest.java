package com.kh.midpoint.game.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadyRequest {

    @NotNull
    private Long playerId;
    private boolean ready;
}

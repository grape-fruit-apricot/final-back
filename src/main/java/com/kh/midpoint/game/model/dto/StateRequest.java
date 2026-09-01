package com.kh.midpoint.game.model.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateRequest {

    @NotNull
    private Long playerId;
    private long version;
    @NotNull
    private Map<String, Object> state;
}

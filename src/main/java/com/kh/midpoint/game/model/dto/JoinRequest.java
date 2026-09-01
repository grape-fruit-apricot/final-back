package com.kh.midpoint.game.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequest {

    @NotBlank(message = "참가자 이름을 입력해 주세요.")
    private String playerName;
}

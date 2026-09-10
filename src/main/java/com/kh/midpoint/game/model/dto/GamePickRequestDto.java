package com.kh.midpoint.game.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 클라이언트가 보내는 값은 주머니 위치 하나뿐이다.
// 누가 골랐는지는 소켓 세션에서 꺼내므로 participantId 를 받지 않는다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GamePickRequestDto {

	@NotNull(message = "주머니를 선택해주세요.")
	@Min(value = 0, message = "올바르지 않은 주머니입니다.")
	private Integer bagIndex;

}

package com.kh.midpoint.game.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 차례가 끝났다고 알릴 때 쓰는 값. 어느 차례를 넘기려는 것인지 명시해야
// 늦게 도착한 알림이 이미 시작된 다음 사람의 차례를 잡아먹지 않는다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GameExpireRequestDto {

	@NotNull
	private Integer turnSeq;

}

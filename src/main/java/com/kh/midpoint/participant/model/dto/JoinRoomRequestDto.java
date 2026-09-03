package com.kh.midpoint.participant.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class JoinRoomRequestDto {

	// 검증 실패 메시지는 GlobalExceptionHandler 를 거쳐 그대로 사용자에게 보이므로 한글로 적는다.
	// NICKNAME 은 VARCHAR2(20 CHAR) 라 길이를 넘기면 DB 까지 내려가 오류가 난다.
	@NotBlank(message = "닉네임을 입력해주세요.")
	@Size(max = 20, message = "닉네임은 20자까지 입력할 수 있습니다.")
	private String nickname;

	@NotNull(message = "출발 위치를 선택해주세요.")
	private Double lat;

	@NotNull(message = "출발 위치를 선택해주세요.")
	private Double lng;

}

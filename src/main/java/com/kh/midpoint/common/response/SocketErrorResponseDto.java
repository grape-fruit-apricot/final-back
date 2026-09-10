package com.kh.midpoint.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// STOMP 는 HTTP 상태코드가 없어 실패 사유를 별도 토픽으로 보내는데, 그때 쓰는 공통 payload.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SocketErrorResponseDto {

	private String message;

}

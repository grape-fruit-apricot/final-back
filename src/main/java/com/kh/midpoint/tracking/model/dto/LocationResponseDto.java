package com.kh.midpoint.tracking.model.dto;

import java.time.LocalDateTime;

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
// 좌표를 보낸 디바이스가 "이제 그만 보내도 되는지"를 판단할 수 있게 돌려준다.
// sessionId 는 디바이스가 방금 보낸 값이라 새로 노출되는 정보가 아니다.
public class LocationResponseDto {

	private Long sessionId;
	private String roomUuid;
	private Double distanceM;
	private String status;
	private LocalDateTime arrivedAt;

}

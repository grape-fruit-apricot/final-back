package com.kh.midpoint.tracking.model.dto;

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
// 디바이스가 "내가 지금 누구를 대신해 어디로 이동하면 되는지"를 받아가는 응답.
// intervalMs 와 thresholdM 은 DB 가 아니라 설정에서 채워 넣는다. 디바이스에 같은 값을
// 두 벌 두면 서버와 어긋나므로, 판정 기준은 언제나 서버가 알려준다.
public class TrackingAssignmentResponseDto {

	private Long sessionId;
	private String roomUuid;
	private String nickname;
	private Double startLat;
	private Double startLng;
	private Double destLat;
	private Double destLng;
	private Integer intervalMs;
	private Double thresholdM;

}

package com.kh.midpoint.room.model.vo;

// 방 단계. DB 는 ROOM.STAGE VARCHAR2 이고 CK_STAGE 가 이 여섯 값만 받는다.
// 이름이 DB 값과 같아야 하므로 바꿀 때는 제약도 같이 본다.
//
// 전에는 여섯 값이 문자열 리터럴로 17줄에 흩어져 있었고, 그중 셋만
// application-constant.yml 에 들어 있었다. 오타가 나도 컴파일은 통과하고
// 런타임에 "올바르지 않은 방 상태" 로만 드러났다.
public enum RoomStage {

	WAITING,
	MODE_SELECTED,
	MIDPOINT_FOUND,
	RESOLVING,
	GAME_PLAYING,
	RESOLVED;

	// VO 와 DTO 의 stage 는 String 그대로 둔다. MyBatis 매핑과 응답 형식을 건드리지 않으려는 것이다.
	// 비교하는 쪽만 이 메서드를 거치면 값의 출처가 하나로 모인다.
	public boolean is(String stage) {
		return name().equals(stage);
	}

	public static boolean isDefined(String stage) {
		for (RoomStage value : values()) {
			if (value.is(stage)) {
				return true;
			}
		}
		return false;
	}

}

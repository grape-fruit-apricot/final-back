package com.kh.midpoint.game.model.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

public class GameDto {

	//DTO 모음 클래스가 생성되지 않도록 막는 생성자
    private GameDto() {}

    /** 새 게임방 생성 시 방장이 입력하는 이름을 받는다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "참가자 이름을 입력해 주세요.")
        private String playerName;
    }

    /** 기존 게임방 참가 시 참가자가 입력하는 이름을 받는다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequest {
        @NotBlank(message = "참가자 이름을 입력해 주세요.")
        private String playerName;
    }

    /** 게임 상태 변경자, 클라이언트가 알고 있는 버전과 새 상태를 받는다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateRequest {
        @NotNull
        private Long playerId;
        private long version;
        @NotNull 
        private Map<String, Object> state;
    }

    /** 특정 참가자의 준비 여부 변경 요청을 받는다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadyRequest {
        @NotNull 
        private Long playerId;
        private boolean ready;
    }

    /** 카운트다운이 끝난 뒤 방장이 강제 시작을 요청할 때 사용한다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForceStartRequest {
        @NotNull
        private Long playerId;
    }

    /** 화면에 필요한 참가자 정보만 노출하는 응답 DTO다. */
    @Value
    public static class PlayerResponse {
        private Long playerId;
        private String name;
        private boolean host;
        private boolean active;
        private boolean ready;
    }

    /** DB 정보와 메모리 런타임 상태를 합쳐 클라이언트에 전달하는 게임 전체 응답이다. */
    @Value
    public static class GameResponse {
        private Long gameId;
        private Long playerId;
        private Long hostId;
        private boolean host;
        private List<PlayerResponse> players;
        private Map<String, Object> gameState;
        private long version;
        private boolean allReady;
        private long hostDelegationSeconds;
        private boolean forceStartEligible;
        private long forceStartSeconds;
    }
}

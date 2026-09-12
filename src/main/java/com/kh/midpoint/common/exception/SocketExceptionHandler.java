package com.kh.midpoint.common.exception;

import com.kh.midpoint.chat.model.vo.ChatSession;
import com.kh.midpoint.common.response.SocketErrorResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Map;

// 소켓 예외 처리를 한곳에 모은다. 전에는 소켓 컨트롤러 6개가 같은 모양의 핸들러를 각자 들고 있었고
// 서로 다른 것은 오류 토픽과 기본 문구뿐이었다. 정책이 흩어져 있으면 한 곳만 고치는 사고가 난다.
//
// REST 는 GlobalExceptionHandler 가 상태 코드로 알려주지만, 소켓은 예외를 그냥 던지면
// 클라이언트가 아무것도 받지 못한 채 멈춘다. 그래서 요청이 들어온 목적지에 맞는 오류 토픽으로
// 되돌려 줘야 한다.
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class SocketExceptionHandler {

	@Value("${chat.session-attribute-key}")
	private String sessionAttributeKey;

	private final SimpMessagingTemplate messagingTemplate;

	// 목적지는 /app/{기능}/{동작} 형태라 두 번째 조각이 곧 기능이다.
	// 기능이 늘면 여기에 한 줄을 더한다. 빠뜨리면 오류가 클라이언트까지 가지 못하므로
	// 아래 findTarget 이 기본값으로 받아내고 경고를 남긴다.
	private static final Map<String, SocketErrorTarget> TARGETS = Map.of(
			"chat", new SocketErrorTarget("/chat/error", "메시지를 처리하지 못했습니다.", "채팅 처리 실패"),
			"mode", new SocketErrorTarget("/mode/error", "투표를 처리하지 못했습니다.", "진행 방식 투표 실패"),
			"result", new SocketErrorTarget("/result/error", "결과를 확정하지 못했습니다.", "결과 확정 실패"),
			"tracking", new SocketErrorTarget("/tracking/error", "이동 추적을 시작하지 못했습니다.", "이동 추적 시작 실패"),
			"game", new SocketErrorTarget("/game/error", "게임을 처리하지 못했습니다.", "게임 처리 실패"),
			"midpoint", new SocketErrorTarget("/midpoint/error", "중간지점을 처리하지 못했습니다.", "중간지점 처리 실패"));

	// 중간지점만 찾기와 재설정이 서로 다른 토픽을 쓴다. 프론트가 두 화면에서 따로 구독한다.
	private static final String MIDPOINT_RESET_DESTINATION = "/app/midpoint/reset";

	private static final SocketErrorTarget MIDPOINT_RESET_TARGET = new SocketErrorTarget(
			"/midpoint/reset/error", "중간지점을 처리하지 못했습니다.", "중간지점 처리 실패");

	private static final SocketErrorTarget UNKNOWN_TARGET = new SocketErrorTarget(
			"/error", "요청을 처리하지 못했습니다.", "소켓 처리 실패");

	@MessageExceptionHandler
	public void handleSocketException(Exception e, SimpMessageHeaderAccessor accessor) {
		ChatSession session = ChatSession.from(accessor, sessionAttributeKey);
		if (session == null) {
			return;
		}

		String destination = accessor.getDestination();
		SocketErrorTarget target = findTarget(destination);

		log.warn("{} - {}", target.logLabel(), e.toString());

		String message = e.getMessage() == null ? target.defaultMessage() : e.getMessage();
		messagingTemplate.convertAndSend("/topic/room/" + session.roomUuid() + target.topicSuffix(),
				new SocketErrorResponseDto(message));
	}

	private SocketErrorTarget findTarget(String destination) {
		if (destination == null) {
			return UNKNOWN_TARGET;
		}
		if (MIDPOINT_RESET_DESTINATION.equals(destination)) {
			return MIDPOINT_RESET_TARGET;
		}

		SocketErrorTarget target = TARGETS.get(findFeature(destination));
		if (target == null) {
			log.warn("오류 토픽이 정해지지 않은 목적지입니다: {}", destination);
			return UNKNOWN_TARGET;
		}
		return target;
	}

	// "/app/game/pick" -> "game"
	private String findFeature(String destination) {
		String[] segments = destination.split("/");
		return segments.length < 3 ? "" : segments[2];
	}

	private record SocketErrorTarget(String topicSuffix, String defaultMessage, String logLabel) {
	}

}

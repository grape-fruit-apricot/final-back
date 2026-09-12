package com.kh.midpoint.game.model.service;

import com.kh.midpoint.game.model.dto.GameStatusDto;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.route.model.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// "게임이 끝나면 승자가 고른 식당을 결과로 박고 경로를 만든다" 는 판단을 들고 있다.
// 전에는 이 판단과 두 호출의 순서가 소켓 컨트롤러에 있었다.
//
// GameService 안에 두지 않은 이유는 insertRoomResult 가 @Transactional 이기 때문이다.
// 같은 클래스에서 부르면 프록시를 거치지 않아 트랜잭션이 적용되지 않는다.
// 이 클래스는 두 서비스를 각각 주입받으므로 호출이 프록시를 그대로 탄다.
//
// 자기 자신은 @Transactional 이 아니다. 경로 만들기가 외부 API 를 여러 번 부르는데
// 그동안 DB 커넥션을 붙잡으면 관계없는 요청까지 대기에 걸린다.
@Service
@RequiredArgsConstructor
public class GameResultResolver {

	@Value("${game.status.finished}")
	private String statusFinished;

	private final GameService gameService;
	private final RouteService routeService;

	// 확정할 것이 없으면 null 을 돌려준다. 알릴 것이 없다는 뜻이다.
	// 상태를 먼저 알린 뒤에 부르도록 나눠 두었다. 경로 계산에 몇 초가 걸려서
	// 한 번에 처리하면 그동안 참가자들이 빈 화면을 보게 된다.
	public RouteResponseDto insertRouteResultIfFinished(String roomUuid, GameStatusDto status) {
		if (!statusFinished.equals(status.getStatus())) {
			return null;
		}

		// 승자가 고른 식당을 결과로 먼저 박아둔다. 그러면 이어지는 insertRouteResult 가
		// 무작위 추첨을 건너뛰고 그 식당으로 경로를 만든다.
		gameService.insertRoomResult(roomUuid);
		return routeService.insertRouteResult(roomUuid);
	}

}

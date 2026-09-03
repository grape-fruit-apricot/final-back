package com.kh.midpoint.route.controller;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.route.model.dto.RouteResponseDto;
import com.kh.midpoint.route.model.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomUuid}/routes")
@RequiredArgsConstructor
public class RouteController {

	private final RouteService routeService;

	// 결과 확정(routeService.findRoute)은 방장만 할 수 있어야 하는데 REST 요청에는 신원이 없다.
	// 그래서 확정은 RouteSocketController 의 /app/result/find 로만 열어두고, 여기서는 조회만 제공한다.
	@GetMapping
	public ResponseEntity<ApiResponse<RouteResponseDto>> findRouteResult(@PathVariable("roomUuid") String roomUuid) {
		RouteResponseDto responseDto = routeService.findRouteResult(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("확정된 결과를 조회했습니다.", responseDto));
	}

}

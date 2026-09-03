package com.kh.midpoint.route.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 방 하나의 경로 좌표를 한 번에 조회할 때 쓰는 조회 결과.
// 어느 경로의 좌표인지 나눠야 하므로 ROUTE_ID 를 함께 받는다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RoutePointQueryDto {

	private Long routeId;
	private Double lat;
	private Double lng;

}

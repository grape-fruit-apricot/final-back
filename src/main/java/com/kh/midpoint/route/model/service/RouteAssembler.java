package com.kh.midpoint.route.model.service;

import com.kh.midpoint.route.model.dao.RouteMapper;
import com.kh.midpoint.route.model.dto.ParticipantRouteQueryDto;
import com.kh.midpoint.route.model.dto.ParticipantRouteResponseDto;
import com.kh.midpoint.route.model.dto.RoutePointDto;
import com.kh.midpoint.route.model.dto.RoutePointQueryDto;
import com.kh.midpoint.route.model.dto.RouteSegmentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 저장된 경로를 응답 모양으로 조립한다. 외부 API 도 결과 확정도 여기서는 하지 않는다.
// RouteService 가 결과 확정, 외부 경로 수집, 응답 조립 셋을 겸하고 있어서
// 조회 결과를 옮겨 담는 일만 떼어냈다.
@Service
@RequiredArgsConstructor
public class RouteAssembler {

	private final RouteMapper routeMapper;

	public List<ParticipantRouteResponseDto> findParticipantRouteList(Long roomId, List<ParticipantRouteQueryDto> routes) {
		// 경로마다 좌표를 따로 조회하면 참가자 수만큼 쿼리가 늘어난다. 도보 폴리라인은
		// 경로당 좌표가 수백 개라 방 단위로 한 번에 가져와 routeId 로 나눈다.
		Map<Long, List<RouteSegmentDto>> segmentsByRouteId = findSegmentsByRouteId(roomId);

		return routes.stream()
				.map(route -> toParticipantRouteResponse(route, segmentsByRouteId.getOrDefault(route.getRouteId(), List.of())))
				.toList();
	}

	private Map<Long, List<RouteSegmentDto>> findSegmentsByRouteId(Long roomId) {
		Map<Long, Map<Long, RouteSegmentDto>> segmentsByRouteAndId = new LinkedHashMap<>();
		for (RoutePointQueryDto point : routeMapper.findRoutePointListByRoom(roomId)) {
			RouteSegmentDto segment = segmentsByRouteAndId
					.computeIfAbsent(point.getRouteId(), routeId -> new LinkedHashMap<>())
					.computeIfAbsent(point.getRouteSegmentId(), routeSegmentId ->
							new RouteSegmentDto(
									point.getSegmentOrder(), point.getSegmentType(),
									point.getSegmentTimeMinutes(), point.getGuidance(),
									findVehicleList(point.getVehicles()), new ArrayList<>()));
			segment.getPoints().add(new RoutePointDto(point.getLat(), point.getLng()));
		}

		Map<Long, List<RouteSegmentDto>> segmentsByRouteId = new LinkedHashMap<>();
		segmentsByRouteAndId.forEach((routeId, segmentsById) ->
				segmentsByRouteId.put(routeId, new ArrayList<>(segmentsById.values())));
		return segmentsByRouteId;
	}

	private List<String> findVehicleList(String vehicles) {
		return vehicles == null || vehicles.isBlank()
				? List.of()
				: List.of(vehicles.split(","));
	}

	private ParticipantRouteResponseDto toParticipantRouteResponse(ParticipantRouteQueryDto route, List<RouteSegmentDto> segments) {
		List<RoutePointDto> points = segments.stream()
				.flatMap(segment -> segment.getPoints().stream())
				.toList();
		return new ParticipantRouteResponseDto(route.getParticipantId(), route.getNickname(), route.getTravelMode(), route.getTimeMinutes(), points, segments);
	}

}

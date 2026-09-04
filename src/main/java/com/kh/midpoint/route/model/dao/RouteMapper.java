package com.kh.midpoint.route.model.dao;

import com.kh.midpoint.route.model.dto.ParticipantRouteQueryDto;
import com.kh.midpoint.route.model.dto.RoutePointQueryDto;
import com.kh.midpoint.route.model.vo.ParticipantRoute;
import com.kh.midpoint.route.model.vo.ParticipantRoutePoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RouteMapper {

	void insertRoute(ParticipantRoute participantRoute);

	void insertRoutePointList(List<ParticipantRoutePoint> routePoints);

	List<ParticipantRouteQueryDto> findRouteList(@Param("roomId") Long roomId,
			@Param("travelMode") String travelMode);

	// 경로별로 따로 조회하면 참가자 수만큼 쿼리가 늘어나므로 방 단위로 한 번에 가져온다.
	List<RoutePointQueryDto> findRoutePointListByRoom(Long roomId);

}

package com.kh.midpoint.route.model.dao;

import com.kh.midpoint.external.tmap.RoutePointDto;
import com.kh.midpoint.route.model.dto.ParticipantRouteQueryDto;
import com.kh.midpoint.route.model.vo.ParticipantRoute;
import com.kh.midpoint.route.model.vo.ParticipantRoutePoint;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RouteMapper {

	void insertRoute(ParticipantRoute participantRoute);

	void insertRoutePointList(List<ParticipantRoutePoint> routePoints);

	List<ParticipantRouteQueryDto> findRouteList(Long roomId);

	List<RoutePointDto> findRoutePointList(Long routeId);

}

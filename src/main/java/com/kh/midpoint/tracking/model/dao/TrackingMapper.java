package com.kh.midpoint.tracking.model.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kh.midpoint.tracking.model.dto.LocationLogResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingAssignmentResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingSessionResponseDto;
import com.kh.midpoint.tracking.model.vo.LocationLog;
import com.kh.midpoint.tracking.model.vo.TrackingSession;

@Mapper
public interface TrackingMapper {

	void insertTrackingSession(TrackingSession trackingSession);

	void insertLocationLog(LocationLog locationLog);

	TrackingSessionResponseDto findTrackingSession(Long sessionId);

	List<TrackingSessionResponseDto> findTrackingSessionList(Long roomId);

	List<TrackingAssignmentResponseDto> findTrackingAssignmentList(String deviceKey);

	List<LocationLogResponseDto> findLocationLogList(Long roomId);

	int updateTrackingProgress(TrackingSession trackingSession);

	int updateTrackingArrived(TrackingSession trackingSession);

}

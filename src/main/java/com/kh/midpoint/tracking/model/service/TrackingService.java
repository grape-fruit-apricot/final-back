package com.kh.midpoint.tracking.model.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.midpoint.common.exception.NotFoundException;
import com.kh.midpoint.common.exception.UnauthorizedException;
import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.restaurant.model.dto.RestaurantResponseDto;
import com.kh.midpoint.room.model.dto.RoomResponseDto;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.roomresult.model.service.RoomResultService;
import com.kh.midpoint.tracking.model.dao.TrackingMapper;
import com.kh.midpoint.tracking.model.dto.LocationLogResponseDto;
import com.kh.midpoint.tracking.model.dto.LocationPointRequestDto;
import com.kh.midpoint.tracking.model.dto.LocationRequestDto;
import com.kh.midpoint.tracking.model.dto.LocationResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingAssignmentResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingDestinationResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingParticipantResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingPointResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingSessionResponseDto;
import com.kh.midpoint.tracking.model.dto.TrackingSummaryResponseDto;
import com.kh.midpoint.tracking.model.vo.LocationLog;
import com.kh.midpoint.tracking.model.vo.TrackingSession;

import lombok.RequiredArgsConstructor;

// 외부 디바이스가 보낸 좌표를 저장하고, 확정 지점까지의 거리를 계산해 도착을 판정한다.
//
// 판정을 서버가 혼자 하는 이유는 디바이스를 믿을 수 없어서다. 디바이스가 "도착했다"를
// 보내는 구조면 그 값을 조작하는 것으로 결과가 바뀐다. 디바이스는 좌표만 보내고,
// 거리·도착 여부는 여기서만 정한다.
@Service
@RequiredArgsConstructor
public class TrackingService {

	@Value("${tracking.arrival-distance-meters}")
	private double arrivalDistanceMeters;
	@Value("${tracking.interval-ms}")
	private int intervalMs;
	@Value("${tracking.status.arrived}")
	private String arrivedStatus;
	@Value("${tracking.device-key}")
	private String deviceKey;
	@Value("${route.earth-radius-meters}")
	private double earthRadiusMeters;

	private final TrackingMapper trackingMapper;
	private final RoomService roomService;
	private final ParticipantService participantService;
	private final RoomResultService roomResultService;

	// 방장이 추적을 시작하면 참가자 전원의 세션을 만든다.
	// 방 행을 잠그는 이유는 방장이 버튼을 두 번 눌렀을 때 세션이 두 벌 생기는 것을 막기 위해서다.
	// (UK_TRACKING_SESSION_PART 가 참가자당 1행만 허용하므로 두 번째는 예외로 죽는다)
	@Transactional
	public TrackingResponseDto insertTrackingSessionList(String roomUuid) {
		RoomResponseDto room = roomService.findRoomForUpdate(roomUuid);
		RestaurantResponseDto destination = findDestination(room.getRoomId());

		// 이미 시작한 방이면 다시 만들지 않고 지금 상태만 돌려준다.
		if (trackingMapper.findTrackingSessionList(room.getRoomId()).isEmpty()) {
			for (ParticipantResponseDto participant : participantService.findParticipantList(roomUuid)) {
				trackingMapper.insertTrackingSession(TrackingSession.builder()
						.roomId(room.getRoomId())
						.participantId(participant.getParticipantId())
						.deviceKey(deviceKey)
						// 목적지는 여기서 값을 복사해 둔다(BR-34). 나중에 결과가 바뀌어도
						// 이동 중인 사람의 판정 기준이 도중에 달라지지 않는다.
						.destLat(destination.getLat())
						.destLng(destination.getLng())
						.build());
			}
		}

		return findTracking(roomUuid);
	}

	// 디바이스가 "내가 지금 무엇을 대신 움직여야 하는지"를 물어보는 자리.
	// 서버가 디바이스에게 먼저 말을 걸 수 없으므로 디바이스 쪽이 물어보는 구조다.
	// 키가 틀리면 조회 결과가 비어 있을 뿐이라 방 정보가 새지 않는다.
	@Transactional(readOnly = true)
	public List<TrackingAssignmentResponseDto> findTrackingAssignmentList(String requestDeviceKey) {
		List<TrackingAssignmentResponseDto> assignments =
				trackingMapper.findTrackingAssignmentList(requestDeviceKey);

		// 전송 주기와 도착 기준은 서버가 알려준다. 디바이스에 같은 값을 두 벌 두면
		// 설정을 바꿨을 때 한쪽만 바뀌어 서로 다른 기준으로 움직이게 된다.
		for (TrackingAssignmentResponseDto assignment : assignments) {
			assignment.setIntervalMs(intervalMs);
			assignment.setThresholdM(arrivalDistanceMeters);
		}

		return assignments;
	}

	@Transactional
	public List<LocationResponseDto> insertLocationLogList(LocationRequestDto requestDto) {
		List<LocationResponseDto> responseDtoList = new ArrayList<>();

		for (LocationPointRequestDto point : requestDto.getPoints()) {
			TrackingSessionResponseDto session = trackingMapper.findTrackingSession(point.getSessionId());
			if (session == null) {
				throw new NotFoundException("존재하지 않는 추적 세션입니다: " + point.getSessionId());
			}
			// 요청에 실려 온 키를 그대로 믿지 않고, 그 세션에 저장된 키와 대조한다.
			// 이게 없으면 세션 번호만 알면 누구든 남의 위치를 밀어넣을 수 있다.
			if (!session.getDeviceKey().equals(requestDto.getDeviceKey())) {
				throw new UnauthorizedException("디바이스 키가 일치하지 않습니다.");
			}

			responseDtoList.add(insertLocationLog(session, point));
		}

		return responseDtoList;
	}

	@Transactional(readOnly = true)
	public TrackingResponseDto findTracking(String roomUuid) {
		RoomResponseDto room = roomService.findRoom(roomUuid);

		List<TrackingSessionResponseDto> sessions = trackingMapper.findTrackingSessionList(room.getRoomId());
		// 세션마다 이력을 따로 읽으면 인원수만큼 쿼리가 늘어난다. 한 번에 읽고 자바에서 나눈다.
		Map<Long, List<LocationLogResponseDto>> logs = trackingMapper.findLocationLogList(room.getRoomId())
				.stream()
				.collect(Collectors.groupingBy(LocationLogResponseDto::getSessionId));

		List<TrackingParticipantResponseDto> participants = sessions.stream()
				.map(session -> toParticipant(session, logs.getOrDefault(session.getSessionId(), List.of())))
				.toList();

		return new TrackingResponseDto(findDestinationOrNull(room.getRoomId()), participants);
	}

	private LocationResponseDto insertLocationLog(TrackingSessionResponseDto session,
			LocationPointRequestDto point) {
		// 도착한 세션은 더 받지 않는다. 상태는 MOVING -> ARRIVED 단방향이라 되돌리지 않는다(BR-35).
		if (arrivedStatus.equals(session.getStatus())) {
			return new LocationResponseDto(session.getSessionId(), session.getRoomUuid(),
					session.getLastDistanceM(), session.getStatus(), session.getArrivedAt());
		}

		double distanceMeters = findDistanceMeters(point.getLat(), point.getLng(),
				session.getDestLat(), session.getDestLng());

		trackingMapper.insertLocationLog(LocationLog.builder()
				.sessionId(session.getSessionId())
				.seq(point.getSeq())
				.lat(point.getLat())
				.lng(point.getLng())
				.distanceM(distanceMeters)
				.build());

		TrackingSession progress = TrackingSession.builder()
				.sessionId(session.getSessionId())
				.lastDistanceM(distanceMeters)
				.build();

		if (distanceMeters > arrivalDistanceMeters) {
			trackingMapper.updateTrackingProgress(progress);
			return new LocationResponseDto(session.getSessionId(), session.getRoomUuid(),
					distanceMeters, session.getStatus(), null);
		}

		trackingMapper.updateTrackingArrived(progress);
		// 도착 시각은 DB 시계로 찍히므로 값을 지어내지 않고 다시 읽어서 돌려준다.
		TrackingSessionResponseDto arrived = trackingMapper.findTrackingSession(session.getSessionId());
		return new LocationResponseDto(arrived.getSessionId(), arrived.getRoomUuid(),
				arrived.getLastDistanceM(), arrived.getStatus(), arrived.getArrivedAt());
	}

	private TrackingParticipantResponseDto toParticipant(TrackingSessionResponseDto session,
			List<LocationLogResponseDto> logs) {
		List<TrackingPointResponseDto> trail = logs.stream()
				.map(log -> new TrackingPointResponseDto(log.getLat(), log.getLng()))
				.toList();
		TrackingPointResponseDto current = trail.isEmpty() ? null : trail.get(trail.size() - 1);

		return new TrackingParticipantResponseDto(
				session.getParticipantId(),
				session.getNickname(),
				session.getStatus(),
				session.getLastDistanceM(),
				current,
				trail,
				session.getArrivedAt(),
				toSummary(session, trail));
	}

	// 도착한 뒤에만 만든다. 이동 중에 내는 평균속도는 아직 끝나지 않은 구간의 값이라 의미가 없다.
	private TrackingSummaryResponseDto toSummary(TrackingSessionResponseDto session,
			List<TrackingPointResponseDto> trail) {
		if (!arrivedStatus.equals(session.getStatus()) || session.getArrivedAt() == null) {
			return null;
		}

		double totalDistanceMeters = 0;
		for (int index = 1; index < trail.size(); index++) {
			TrackingPointResponseDto previous = trail.get(index - 1);
			TrackingPointResponseDto point = trail.get(index);
			totalDistanceMeters += findDistanceMeters(previous.getLat(), previous.getLng(),
					point.getLat(), point.getLng());
		}

		long durationSeconds = Duration.between(session.getStartedAt(), session.getArrivedAt()).toSeconds();
		// 0초 안에 도착하면(좌표를 한 번에 몰아 보낸 경우) 나눗셈이 무한대가 된다.
		double averageSpeedKmh = durationSeconds > 0 ? totalDistanceMeters / durationSeconds * 3.6 : 0;

		return new TrackingSummaryResponseDto(round(totalDistanceMeters), durationSeconds,
				round(averageSpeedKmh));
	}

	private RestaurantResponseDto findDestination(Long roomId) {
		RestaurantResponseDto destination = roomResultService.findRoomResult(roomId);
		if (destination == null) {
			throw new NotFoundException("아직 확정된 결과가 없습니다.");
		}
		return destination;
	}

	// 조회는 결과가 없어도 화면이 떠야 하므로 예외 대신 null 을 준다.
	private TrackingDestinationResponseDto findDestinationOrNull(Long roomId) {
		RestaurantResponseDto destination = roomResultService.findRoomResult(roomId);
		if (destination == null) {
			return null;
		}
		return new TrackingDestinationResponseDto(destination.getName(), destination.getLat(),
				destination.getLng());
	}

	// 소수점 첫째 자리까지만 남긴다. 화면에 "320.53812 m" 를 띄울 이유가 없다.
	private double round(double value) {
		return Math.round(value * 10) / 10.0;
	}

	// 위경도의 직선거리를 미터로 변환한다(Haversine).
	// MidPointService 에도 같은 계산이 private 으로 있다. 공용 유틸로 뽑는 것은
	// 새 클래스를 만드는 일이라 팀 합의가 필요해, 이 PR 에서는 선례대로 각자 들고 간다.
	private double findDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
		double latSin = Math.sin(Math.toRadians(lat2 - lat1) / 2);
		double lngSin = Math.sin(Math.toRadians(lng2 - lng1) / 2);
		double a = latSin * latSin
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * lngSin * lngSin;
		return earthRadiusMeters * 2 * Math.asin(Math.sqrt(Math.min(1.0, Math.max(0.0, a))));
	}

}

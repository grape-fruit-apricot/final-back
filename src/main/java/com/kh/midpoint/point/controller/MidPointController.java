package com.kh.midpoint.point.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.external.kakao.NearbyStationDto;
import com.kh.midpoint.point.model.service.MidPointService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class MidPointController {
	
	private final MidPointService midPointService;
	
	@PostMapping("/{roomUuid}/midpoint")
	public ResponseEntity<ApiResponse<NearbyStationDto>> findMidpoint(@PathVariable("roomUuid") String roomUuid) {

	    NearbyStationDto midpoint = midPointService.findMidpoint(roomUuid);

	    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("중간지점을 찾았습니다", midpoint));
	}

}

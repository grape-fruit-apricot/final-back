package com.kh.midpoint.vote.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.midpoint.common.response.ApiResponse;
import com.kh.midpoint.vote.model.dto.ModeVoteStatusDto;
import com.kh.midpoint.vote.model.service.ModeVoteService;

import lombok.RequiredArgsConstructor;

// 새로고침이나 뒤늦은 입장에서 투표 현황을 복원하기 위한 조회 전용 경로.
// 투표 자체는 소켓으로만 받는다(작성자를 세션에서 확인해야 하므로).
@RestController
@RequestMapping("/api/rooms/{roomUuid}/votes")
@RequiredArgsConstructor
public class ModeVoteController {

	private final ModeVoteService modeVoteService;

	@GetMapping
	public ResponseEntity<ApiResponse<ModeVoteStatusDto>> findModeVoteStatus(
			@PathVariable("roomUuid") String roomUuid) {
		ModeVoteStatusDto responseDto = modeVoteService.findModeVoteStatus(roomUuid);
		return ResponseEntity.ok(ApiResponse.ok("투표 현황 조회에 성공했습니다.", responseDto));
	}

}

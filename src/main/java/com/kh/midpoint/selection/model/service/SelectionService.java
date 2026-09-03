package com.kh.midpoint.selection.model.service;

import com.kh.midpoint.common.exception.InvalidStateException;
import com.kh.midpoint.participant.model.service.ParticipantService;
import com.kh.midpoint.room.model.service.RoomService;
import com.kh.midpoint.selection.model.dao.SelectionMapper;
import com.kh.midpoint.selection.model.dto.SelectionRequestDto;
import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.vo.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SelectionService {

	private final SelectionMapper selectionMapper;
	private final RoomService roomService;
	private final ParticipantService participantService;

	@Transactional
	public SelectionResponseDto selectRestaurant(String roomUuid, Long participantId, SelectionRequestDto requestDto) {
		String stage = roomService.findRoom(roomUuid).getStage();
		if (!"MIDPOINT_FOUND".equals(stage)) {
			throw new InvalidStateException("중간 지점이 결정된 상태에서만 식당을 선택할 수 있습니다.");
		}

		participantService.validateParticipant(roomUuid, participantId);

		Selection selection = Selection.builder()
			.participantId(participantId)
			.restaurantId(requestDto.getRestaurantId())
			.build();

		selectionMapper.upsertSelection(selection);

		return selectionMapper.findSelection(participantId);
	}

	@Transactional(readOnly = true)
	public SelectionResponseDto findSelection(Long participantId) {
		return selectionMapper.findSelection(participantId);
	}

	@Transactional(readOnly = true)
	public List<SelectionResponseDto> findSelectionList(String roomUuid) {
		roomService.findRoom(roomUuid);
		return selectionMapper.findSelectionList(roomUuid);
	}

}

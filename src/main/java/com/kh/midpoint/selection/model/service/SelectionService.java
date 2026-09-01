package com.kh.midpoint.selection.model.service;

import com.kh.midpoint.selection.model.dao.SelectionMapper;
import com.kh.midpoint.selection.model.dto.SelectionRequestDto;
import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.vo.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SelectionService {

	private final SelectionMapper selectionMapper;

	@Transactional
	public SelectionResponseDto selectRestaurant(Long participantId, SelectionRequestDto requestDto) {
		Selection selection = Selection.builder()
			.participantId(participantId)
			.restaurantId(requestDto.getRestaurantId())
			.build();

		if (selectionMapper.findSelection(participantId) == null) {
			selectionMapper.insertSelection(selection);
		} else {
			selectionMapper.updateSelection(selection);
		}

		return selectionMapper.findSelection(participantId);
	}

}

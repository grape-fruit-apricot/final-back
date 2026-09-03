package com.kh.midpoint.selection.model.dao;

import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.vo.Selection;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SelectionMapper {

	SelectionResponseDto findSelection(Long participantId);

	List<SelectionResponseDto> findSelectionList(String roomUuid);

	void insertSelection(Selection selection);

}

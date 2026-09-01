package com.kh.midpoint.selection.model.dao;

import com.kh.midpoint.selection.model.dto.SelectionResponseDto;
import com.kh.midpoint.selection.model.vo.Selection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SelectionMapper {

	SelectionResponseDto findSelection(Long participantId);

	void upsertSelection(Selection selection);

}

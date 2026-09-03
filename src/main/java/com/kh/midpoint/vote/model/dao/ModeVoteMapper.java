package com.kh.midpoint.vote.model.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kh.midpoint.vote.model.dto.ModeVoteResponseDto;
import com.kh.midpoint.vote.model.vo.ModeVote;

@Mapper
public interface ModeVoteMapper {

	void insertModeVote(ModeVote modeVote);

	List<ModeVoteResponseDto> findModeVoteList(String roomUuid);

	void deleteModeVoteList(String roomUuid);

}

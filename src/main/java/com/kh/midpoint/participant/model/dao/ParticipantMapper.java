package com.kh.midpoint.participant.model.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kh.midpoint.participant.model.dto.ParticipantResponseDto;
import com.kh.midpoint.participant.model.vo.Participant;

@Mapper
public interface ParticipantMapper {

	void insertParticipant(Participant participant);

	ParticipantResponseDto findParticipant(Long participantId);

	void deleteParticipant(Long participantId);

	void updateNextHost(Long roomId);

	int updateReady(Participant participant);
	
	List<ParticipantResponseDto> findParticipantList(Long roomId);

}

package com.kh.midpoint.participant.model.dao;

import org.apache.ibatis.annotations.Mapper;

import com.kh.midpoint.participant.model.vo.Participant;

@Mapper
public interface ParticipantMapper {
	
	void insertParticipant(Participant participant);

}

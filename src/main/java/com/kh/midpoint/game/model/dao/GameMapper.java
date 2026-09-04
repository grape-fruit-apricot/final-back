package com.kh.midpoint.game.model.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kh.midpoint.game.model.dto.GamePickResponseDto;
import com.kh.midpoint.game.model.dto.GamePlayerResponseDto;
import com.kh.midpoint.game.model.dto.GameQueryDto;
import com.kh.midpoint.game.model.dto.GameStatusDto;
import com.kh.midpoint.game.model.dto.GameWinnerQueryDto;
import com.kh.midpoint.game.model.vo.Game;
import com.kh.midpoint.game.model.vo.GameParticipant;
import com.kh.midpoint.game.model.vo.GamePick;

@Mapper
public interface GameMapper {

	void insertGame(Game game);

	void insertGameParticipantList(List<GameParticipant> gameParticipantList);

	void insertGamePick(GamePick gamePick);

	// 게임 행을 잠가서 읽는다. 같은 방에 대한 변경을 한 줄로 세우는 지점이다.
	GameQueryDto findGameForUpdate(Long roomId);

	GameQueryDto findGame(Long roomId);

	GameStatusDto findGameStatus(Long roomId);

	List<GamePlayerResponseDto> findGamePlayerList(Long roomId);

	List<GamePickResponseDto> findGamePickList(Long roomId);

	// 아직 나가지 않은 참가자를 차례 순서대로 준다. 다음 차례 계산에 쓴다.
	List<Long> findActiveParticipantIdList(Long roomId);

	GameWinnerQueryDto findGameWinner(Long roomId);

	// 아래 update 들은 조건이 맞은 행 수를 돌려준다. 0 이면 내 차례가 아니거나 이미 지나간 요청이다.
	int updateTurnByPick(@Param("roomId") Long roomId, @Param("participantId") Long participantId,
			@Param("nextParticipantId") Long nextParticipantId, @Param("turnSeconds") Integer turnSeconds);

	int updateTurnByTimeout(@Param("roomId") Long roomId, @Param("turnSeq") Integer turnSeq,
			@Param("nextParticipantId") Long nextParticipantId, @Param("turnSeconds") Integer turnSeconds);

	// 나가거나 연결이 끊긴 사람의 차례를 즉시 넘긴다. 마감 시각을 기다릴 이유가 없으므로 조건에서 뺀다.
	int updateTurnByLeave(@Param("roomId") Long roomId, @Param("participantId") Long participantId,
			@Param("nextParticipantId") Long nextParticipantId, @Param("turnSeconds") Integer turnSeconds);

	// 당첨 위치 비교를 여기서만 한다. 1 이면 당첨이라 게임이 끝난다.
	int updateGameFinished(@Param("roomId") Long roomId, @Param("bagIndex") Integer bagIndex);

	int updateGameAborted(Long roomId);

	void updateGameWinner(Long participantId);

	int updateGameParticipantLeft(Long participantId);

	void deleteGame(Long roomId);

}

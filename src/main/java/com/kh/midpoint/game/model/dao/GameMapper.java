package com.kh.midpoint.game.model.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kh.midpoint.game.model.vo.Game;
import com.kh.midpoint.game.model.vo.GamePlayer;

import java.util.List;

/**
 * 게임방, 참가자와 게임 결과에 필요한 SQL을 MyBatis XML과 연결하는 데이터 접근 인터페이스다.
 * 메서드 이름과 파라미터는 GameMapper.xml의 statement id 및 파라미터명과 대응한다.
 */
@Mapper
public interface GameMapper {
    //새로운 게임 방을 등록하는 메서드
    //게임 방에 새로운 참가자를 등록하는 메서드
    int insertPlayer(GamePlayer player);
    //게임 ID로 방 정보를 조회하는 메서드
    Game findGame(Long gameId);
    //동시 참가 처리를 위해 게임 방을 잠금 조회하는 메서드
    Game findGameForUpdate(Long gameId);
    //게임에 남아 있는 활성 참가자 목록을 조회하는 메서드
    List<GamePlayer> findPlayerList(Long gameId);
    //게임에서 동일한 닉네임이 사용된 횟수를 조회하는 메서드
    boolean existsPlayerName(@Param("gameId") Long gameId, @Param("playerName") String playerName);
    //다음 참가자의 입장 순서를 계산하는 메서드
    int findNextPlayerOrder(Long gameId);
    //참가자의 퇴장 시각을 기록해 비활성 상태로 변경하는 메서드
    int deletePlayer(@Param("gameId") Long gameId, @Param("playerId") String playerId);
    //게임에 속한 모든 참가자 데이터를 삭제하는 메서드
    int deletePlayerList(Long gameId);
    //현재 남아 있는 참가자 중 다음 방장 ID를 조회하는 메서드
    String findNextHostId(Long gameId);
    //지정한 참가자에게 방장 권한을 변경하는 메서드
    int updateHost(@Param("gameId") Long gameId, @Param("hostPlayerId") String hostPlayerId);
    //게임 방 데이터를 삭제하는 메서드
    int deleteGame(Long gameId);

  
}

	

	
        

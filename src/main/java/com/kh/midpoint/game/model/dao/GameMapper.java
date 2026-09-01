package com.kh.midpoint.game.model.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kh.midpoint.game.model.vo.Game;
import com.kh.midpoint.game.model.vo.GamePlayer;

@Mapper
public interface GameMapper {

    int insertPlayer(GamePlayer player);

    Game findGame(@Param("roomId") Long roomId);

    Game findGameForUpdate(@Param("roomId") Long roomId);

    List<GamePlayer> findPlayerList(@Param("roomId") Long roomId);

    boolean existsPlayerName(@Param("roomId") Long roomId,
            @Param("playerName") String playerName);

    int findNextPlayerOrder(@Param("roomId") Long roomId);

    int deletePlayer(@Param("roomId") Long roomId,
            @Param("playerId") String playerId);

    int deletePlayerList(@Param("roomId") Long roomId);

    String findNextHostId(@Param("roomId") Long roomId);

    int updateHost(@Param("roomId") Long roomId,
            @Param("hostPlayerId") String hostPlayerId);

    int deleteGame(@Param("roomId") Long roomId);
}

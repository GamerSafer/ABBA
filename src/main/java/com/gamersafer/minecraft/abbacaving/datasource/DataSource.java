package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.PlayerScoreEntry;
import com.gamersafer.minecraft.abbacaving.player.PlayerData;

import java.util.UUID;

public interface DataSource {

    void init();

    PlayerData loadPlayerData(UUID uuid);

    void savePlayerStats(PlayerData stats);

    void savePlayerHotbar(PlayerData stats);

    void savePlayerRespawns(PlayerData stats);

    void savePlayerCosmetics(PlayerData stats);

    void saveFinishedGame(Game game);

    PlayerScoreEntry winEntry(String gameId, int place);

    PlayerScoreEntry globalScoreEntry(int place);

    PlayerScoreEntry globalWinEntry(int place);

    PlayerScoreEntry globalRoundsEntry(int place);

    PlayerScoreEntry globalAverageRoundScore(int place);

    PlayerScoreEntry globalBlockPlaceEntry(int place);

    void purge();

}


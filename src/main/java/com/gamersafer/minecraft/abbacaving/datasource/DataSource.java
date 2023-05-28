package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.PlayerWinEntry;
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

    PlayerWinEntry winEntry(String gameId, int place);

    PlayerWinEntry globalWinEntry(int place);

    PlayerWinEntry globalBlockPlaceEntry(int place);

    void purge();

}


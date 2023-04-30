package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.PlayerWinEntry;

public interface DataSource {

    void init();

    void loadPlayerStats(final GamePlayer gp);

    void savePlayerStats(final GamePlayer gp);

    void savePlayerHotbar(final GamePlayer gp);

    void updatePlayerRespawns(final GamePlayer gp);

    void savePlayerRespawns(final GamePlayer gp);

    void savePlayerCosmetics(final GamePlayer gp);

    void saveFinishedGame(Game game);

    PlayerWinEntry winEntry(String gameId, int place);

    PlayerWinEntry globalWinEntry(int place);

}


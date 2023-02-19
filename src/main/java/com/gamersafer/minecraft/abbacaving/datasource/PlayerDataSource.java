package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.game.GamePlayer;

public interface PlayerDataSource {

    void init();

    void loadPlayerStats(final GamePlayer gp);

    void savePlayerStats(final GamePlayer gp);

    void savePlayerHotbar(final GamePlayer gp);

    void updatePlayerRespawns(final GamePlayer gp);

    void savePlayerRespawns(final GamePlayer gp);

    void savePlayerCosmetics(final GamePlayer gp);

    void updatePlayerRespawns(final GamePlayer gp);

    void savePlayerRespawns(final GamePlayer gp);

}

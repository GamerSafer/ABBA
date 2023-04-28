package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.PlayerWinEntry;

public class DummyDataSource implements DataSource {

    @Override
    public void init() {

    }

    @Override
    public void loadPlayerStats(final GamePlayer gp) {
    }

    @Override
    public void savePlayerStats(final GamePlayer gp) {

    }

    @Override
    public void savePlayerHotbar(final GamePlayer gp) {

    }

    @Override
    public void updatePlayerRespawns(final GamePlayer gp) {

    }

    @Override
    public void savePlayerRespawns(final GamePlayer gp) {

    }

    @Override
    public void savePlayerCosmetics(final GamePlayer gp) {

    }

    @Override
    public void saveFinishedGame(final Game game) {

    }

    @Override
    public PlayerWinEntry winEntry(final String gameId, final int place) {
        return null;
    }

}

package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.PlayerWinEntry;

public class DummyDataSource implements DataSource {
    @Override
    public void init() {

    }

    @Override
    public void loadPlayerStats(GamePlayer gp) {
    }

    @Override
    public void savePlayerStats(GamePlayer gp) {

    }

    @Override
    public void savePlayerHotbar(GamePlayer gp) {

    }

    @Override
    public void updatePlayerRespawns(GamePlayer gp) {

    }

    @Override
    public void savePlayerRespawns(GamePlayer gp) {

    }

    @Override
    public void savePlayerCosmetics(GamePlayer gp) {

    }

    @Override
    public void saveFinishedGame(Game game) {

    }

    @Override
    public PlayerWinEntry getWinEntry(String gameId, int place) {
        return null;
    }
}

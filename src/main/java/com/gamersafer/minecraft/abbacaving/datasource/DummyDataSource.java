package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.PlayerScoreEntry;
import com.gamersafer.minecraft.abbacaving.player.PlayerData;

import java.util.UUID;

public class DummyDataSource implements DataSource {

    @Override
    public void init() {

    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        return new PlayerData(uuid);
    }

    @Override
    public void savePlayerStats(PlayerData stats) {

    }

    @Override
    public void savePlayerHotbar(PlayerData stats) {

    }

    @Override
    public void savePlayerRespawns(PlayerData stats) {

    }

    @Override
    public void savePlayerCosmetics(PlayerData stats) {

    }


    @Override
    public void saveFinishedGame(final Game game) {

    }

    @Override
    public PlayerScoreEntry winEntry(final String gameId, final int place) {
        return null;
    }

    @Override
    public PlayerScoreEntry globalScoreEntry(int place) {
        return null;
    }

    @Override
    public PlayerScoreEntry globalWinEntry(int place) {
        return null;
    }

    @Override
    public PlayerScoreEntry globalRoundsEntry(int place) {
        return null;
    }

    @Override
    public PlayerScoreEntry globalAverageRoundScore(int place) {
        return null;
    }

    @Override
    public PlayerScoreEntry globalBlockPlaceEntry(int place) {
        return null;
    }

    @Override
    public void purge() {

    }

    @Override
    public void delete(UUID uuid) {

    }

}

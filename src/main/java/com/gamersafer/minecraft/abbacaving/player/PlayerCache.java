package com.gamersafer.minecraft.abbacaving.player;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerCache {

    private final AbbaCavingPlugin plugin;
    private final Map<UUID, GamePlayer> playerCache = new HashMap<>();

    public PlayerCache(AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    public GamePlayer getLoaded(UUID uuid) {
        GamePlayer gamePlayer = this.playerCache.get(uuid);
        if (gamePlayer == null) {
            throw new IllegalStateException("Expected player to be loaded!");
        }

        return gamePlayer;
    }

    public void preload(UUID uuid) {
        GamePlayer gamePlayer = new GamePlayer(this.plugin, uuid, this.plugin.playerDataSource().loadPlayerData(uuid));
        this.playerCache.put(uuid, gamePlayer);
    }

    public void unloadAndCompleteAsync(UUID uuid, Consumer<GamePlayer> playerConsumer) {
        GamePlayer gamePlayer = this.playerCache.remove(uuid);
        if (gamePlayer != null) {
            playerConsumer.accept(gamePlayer);
        }
    }


    public void unload(UUID uuid) {
        this.playerCache.remove(uuid);
    }

    public Iterable<? extends GamePlayer> values() {
        return this.playerCache.values();
    }
}

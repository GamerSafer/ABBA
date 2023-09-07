package com.gamersafer.minecraft.abbacaving.player;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.ForwardingLoadingCache;
import com.google.common.cache.LoadingCache;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PlayerCache {

    private final AbbaCavingPlugin plugin;
    private final Map<UUID, GamePlayer> playerCache = new HashMap<>();
    private final Cache<UUID, GamePlayer> temporalLoadingCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();


    public PlayerCache(AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    public GamePlayer getLoaded(Player player) {
        return this.getLoaded(player.getUniqueId());
    }

    public GamePlayer getLoaded(UUID uuid) {
        GamePlayer gamePlayer = this.playerCache.get(uuid);
        if (gamePlayer == null) {
            throw new IllegalStateException("Expected player to be loaded!");
        }

        return gamePlayer;
    }


    public void preload(UUID uuid) {
        this.temporalLoadingCache.put(uuid, this.preloadPlayer(uuid));
    }

    private GamePlayer preloadPlayer(UUID uuid) {
        return new GamePlayer(this.plugin, uuid, this.plugin.playerDataSource().loadPlayerData(uuid));
    }

    public void unloadAndComplete(UUID uuid, Consumer<GamePlayer> playerConsumer) {
        GamePlayer gamePlayer = this.playerCache.remove(uuid);
        if (gamePlayer != null) {
            playerConsumer.accept(gamePlayer);
        }
    }


    public void unload(UUID uuid) {
        this.playerCache.remove(uuid);
    }

    public Collection<? extends GamePlayer> values() {
        return this.playerCache.values();
    }

    public void joinPreloadedOrLoad(UUID uniqueId) {
        GamePlayer gamePlayer = this.temporalLoadingCache.getIfPresent(uniqueId);;
        if (gamePlayer == null) {
            gamePlayer = this.preloadPlayer(uniqueId);
        } else {
            this.temporalLoadingCache.invalidate(uniqueId);
        }

        this.playerCache.put(uniqueId, gamePlayer);
    }
}

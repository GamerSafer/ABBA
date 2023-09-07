package com.gamersafer.minecraft.abbacaving.game.map;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import com.gamersafer.minecraft.abbacaving.lobby.QueueState;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapPool {

    private final AbbaCavingPlugin plugin;
    private final Logger logger;
    private final ConfigurationSection mapSettings;
    private final List<GameMap> mapPool = new ArrayList<>();
    private final Map<String, GameMap> gameMapLookup = new HashMap<>();

    public MapPool(Logger logger, ConfigurationSection mapSettings, AbbaCavingPlugin plugin) {
        this.logger = logger;
        this.mapSettings = mapSettings;
        GameMap.purgeOldWorlds();
        for (final String mapName : this.configuredMapNames()) {
            GameMap game = new GameMap(mapName, this.mapSettings, this.logger);
            this.mapPool.add(game);
            this.gameMapLookup.put(mapName, game);
        }
        this.plugin = plugin;
    }

    public Game startGame(GameMap map) {
        Game gameCheck = this.plugin.gameTracker().getGame(map);
        if (gameCheck != null) {
            return null;
        }
        Game game = new Game(this.plugin, map);
        this.plugin.gameTracker().addGame(map, game);

        return game;
    }

    public List<String> configuredMapNames() {
        final List<String> mapNames = new ArrayList<>();

        for (final String key : this.mapSettings.getKeys(false)) {
            if (!key.equals("default-settings")) {
                mapNames.add(key);
            }
        }

        return mapNames;
    }

    public Collection<GameMap> getMaps() {
        return this.mapPool;
    }

    @Nullable
    public GameMap getMap(String mapName) {
        return this.gameMapLookup.get(mapName);
    }

    public void releaseGame(Game game) {
        GameMap map = game.getMap();
        this.plugin.gameTracker().remove(map);

        map.reloadMap()
                .thenAccept((action) -> {

                    final LobbyQueue queue = this.plugin.lobby().lobbyQueue(map);

                    if (queue != null) {
                        queue.setState(QueueState.WAITING);
                    }
                })
                .exceptionally((e) -> {
                    logger.log(Level.INFO, "Failed to load world, unregistering map.", e);
                    this.gameMapLookup.remove(map.getName());
                    this.mapPool.remove(map);
                    return null;
                });

    }
}

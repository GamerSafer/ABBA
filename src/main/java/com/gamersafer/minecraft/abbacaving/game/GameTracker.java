package com.gamersafer.minecraft.abbacaving.game;

import com.gamersafer.minecraft.abbacaving.game.map.GameMap;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GameTracker {

    private final List<Game> currentGames = new ArrayList<>();
    private final Map<String, Game> mapNameToGame = new HashMap<>();
    private final Map<GameMap, Game> mapToGame = new HashMap<>();
    private final Map<World, Game> worldToGame = new HashMap<>();
    private final Map<UUID, Game> playerToGame = new HashMap<>();

    public List<Game> currentGames() {
        return this.currentGames;
    }

    @Nullable
    public Game gameByMapName(final String mapName) {
        return this.mapNameToGame.get(mapName);
    }

    @Nullable
    public Game findGame(final GamePlayer player) {
        return this.findGame(player.player().getUniqueId());
    }

    @Nullable
    public Game findGame(final Player player) {
        return this.findGame(player.getUniqueId());
    }

    @Nullable
    public Game findGame(final UUID player) {
        return this.playerToGame.get(player);
    }

    @Nullable
    public Game getGame(GameMap map) {
        return this.mapToGame.get(map);
    }

    @Nullable
    public Game getGame(World world) {
        return this.worldToGame.get(world);
    }

    public void addGame(GameMap map, Game game) {
        this.currentGames.add(game);
        this.mapNameToGame.put(map.getName(), game);
        this.mapToGame.put(map, game);
        this.worldToGame.put(map.getWorld(), game);
    }

    public void remove(GameMap map) {
        Game game = this.mapToGame.remove(map);
        this.currentGames.remove(game);
        this.mapNameToGame.remove(map.getName());
        this.worldToGame.remove(map.getWorld());
    }

    public void registerPlayerGame(UUID uuid, Game game) {
        this.playerToGame.put(uuid, game);
    }

    public void unregisterPlayerGame(UUID uuid, Game game) {
        this.playerToGame.remove(uuid, game);
    }
}

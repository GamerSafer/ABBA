package com.gamersafer.minecraft.abbacaving.game;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class GameTracker {

    private final AbbaCavingPlugin plugin;
    private final List<Game> currentGames = new ArrayList<>();
    private final Map<UUID, GamePlayer> playerCache = new HashMap<>();

    public GameTracker(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    public List<Game> currentGames() {
        return this.currentGames;
    }

    public void addPlayerToGame(final Game game, final Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        game.addPlayer(this.gamePlayer(player));
    }

    public Game gameById(final String gameId) {
        for (final Game game : this.currentGames()) {
            if (game.gameId().equals(gameId.toUpperCase())) {
                return game;
            }
        }

        return null;
    }

    public Game gameByMapName(final String mapName) {
        for (final Game game : this.currentGames()) {
            if (game.mapName().equals(mapName)) {
                return game;
            }
        }

        return null;
    }

    public Game findGame(final World world) {
        for (final Game game : this.currentGames()) {
            if (game.world().equals(world)) {
                return game;
            }
        }

        return null;
    }

    public Game findGame(final Player player) {
        for (final Game game : this.currentGames()) {
            if (game.player(player) != null) {
                return game;
            }
        }

        return null;
    }

    public GamePlayer gamePlayer(final UUID uuid) {
        return this.playerCache.computeIfAbsent(uuid, _uuid -> new GamePlayer(this.plugin, _uuid));
    }

    public GamePlayer gamePlayer(final Player player) {
        return this.playerCache.computeIfAbsent(player.getUniqueId(), uuid -> new GamePlayer(this.plugin, uuid));
    }

    public GamePlayer findPlayerInGame(final Player player) {
        for (final Game game : this.currentGames()) {
            final GamePlayer gamePlayer = game.player(player);

            if (gamePlayer != null) {
                return gamePlayer;
            }
        }

        return null;
    }

    public GamePlayer removePlayer(final Player player, final boolean quit) {
        for (final Game game : this.currentGames()) {
            final GamePlayer gamePlayer = game.removePlayer(player, quit);

            if (gamePlayer != null) {
                return gamePlayer;
            }
        }

        return null;
    }

}

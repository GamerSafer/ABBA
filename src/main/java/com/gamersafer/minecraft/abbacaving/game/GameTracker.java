package com.gamersafer.minecraft.abbacaving.game;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class GameTracker {

    private final AbbaCavingPlugin plugin;
    private final List<Game> currentGames = new ArrayList<>();

    public GameTracker(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    public List<Game> currentGames() {
        return this.currentGames;
    }

    public void addPlayerToGame(final Game game, final Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        //player.teleport(game.world().getSpawnLocation());

        final GamePlayer gp = new GamePlayer(this.plugin, player);
        game.addPlayer(gp);

        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.playerDataSource().loadPlayerStats(gp));
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

    public GamePlayer findPlayer(final Player player) {
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

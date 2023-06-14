package com.gamersafer.minecraft.abbacaving.placeholders;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.game.PlayerScoreEntry;
import com.gamersafer.minecraft.abbacaving.game.map.GameMap;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.player.GameStats;
import com.gamersafer.minecraft.abbacaving.util.Util;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GamePlaceholders extends PlaceholderExpansion {

    private final ScoreboardProvider[] scoreboards;
    private final AbbaCavingPlugin plugin;

    public GamePlaceholders(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
        this.scoreboards = new ScoreboardProvider[]{
                this.genericScoreboard("global_leaderboard_score_", place -> plugin.playerDataSource().globalScoreEntry(place)),
                this.genericScoreboard("global_win_leaderboard_score_", place -> plugin.playerDataSource().globalWinEntry(place)),
                this.genericScoreboard("global_block_leaderboard_score_", place -> plugin.playerDataSource().globalBlockPlaceEntry(place)),
                this.genericScoreboard("global_average_leaderboard_score_", place -> plugin.playerDataSource().globalAverageRoundScore(place)),
                this.genericScoreboard("global_rounds_leaderboard_score_", place -> plugin.playerDataSource().globalRoundsEntry(place)),
        };
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "abbacaving";
    }

    @Override
    public String getAuthor() {
        return "Cole Bennett";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @SuppressWarnings("checkstyle:FallThrough")
    @Override
    public String onPlaceholderRequest(final Player player, final String identifier) {
        final GamePlayer gp = this.plugin.getPlayerCache().getLoaded(player.getUniqueId());
        final Game game = this.plugin.gameTracker().findGame(player);

        if ("online".equals(identifier)) {
            return Integer.toString(Bukkit.getServer().getOnlinePlayers().size());
        }

        if (identifier.startsWith("map_")) {
            final String trimmed = identifier.substring("map_".length());

            if (!trimmed.contains("_")) {
                return "";
            }

            final String mapName = trimmed.substring(0, trimmed.lastIndexOf("_"));
            final String suffix = trimmed.substring(trimmed.lastIndexOf("_") + 1);

            GameMap gameMap = this.plugin.getMapPool().getMap(mapName);
            if (gameMap != null) {
                final LobbyQueue queue = this.plugin.lobby().lobbyQueue(gameMap);
                if (queue == null) {
                    return "";
                }

                return switch (suffix) {
                    case "state" -> queue.getState().displayName();
                    case "players" -> Integer.toString(queue.getPlayerQueue().size());
                    case "slots" -> Integer.toString(queue.getMap().maxPlayers());
                    case "counter" -> Integer.toString(queue.getCounter());
                    case "required" -> Integer.toString(queue.getMap().getStartTime());
                    default -> "";
                };
            }
        }
        if (identifier.startsWith("game_")) {
            // Game only
            if (game != null) {
                switch (identifier) {
                    case "game_name" -> {
                        return game.getMap().getName();
                    }
                    case "game_players" -> {
                        return Integer.toString(game.players().size());
                    }
                    case "game_maxplayers" -> {
                        return Integer.toString(game.maxPlayersPerRound());
                    }
                    case "game_id" -> {
                        return game.gameId();
                    }
                    case "game_state" -> {
                        return game.gameState().displayName();
                    }
                }
            }

            final String path = identifier.replace("game_", "");
            final String[] tokens = path.split("_");
            final String gameId = tokens[0];
            // Game only
            if (game != null) {
                switch (identifier) {
                    case "game_players" -> {
                        return Integer.toString(game.players().size());
                    }
                    case "game_maxplayers" -> {
                        return Integer.toString(game.maxPlayersPerRound());
                    }
                    case "map_name" -> {
                        return game.getMap().getName();
                    }
                    case "game_id" -> {
                        return game.gameId();
                    }
                    case "game_state" -> {
                        return game.gameState().displayName();
                    }
                }
            }

            if (tokens.length >= 2 && tokens[1].equals("leaderboard")) {
                final int place = Integer.parseInt(tokens[2]);
                final PlayerScoreEntry winEntry = this.plugin.playerDataSource().winEntry(gameId, place - 1);
                if (winEntry == null) {
                    return "";
                }

                switch (tokens[3]) {
                    case "playername" -> {
                        final PlayerProfile playerProfile = Bukkit.createProfile(winEntry.player());
                        playerProfile.complete();

                        return playerProfile.getName();
                    }
                    case "playeruuid" -> {
                        return winEntry.player().toString();
                    }
                    case "score" -> {
                        return Integer.toString(winEntry.score());
                    }
                }
            }

            return "";
        }

        for (ScoreboardProvider provider : this.scoreboards) {
            String provided = provider.apply(identifier);
            if (provided != null) {
                return provided;
            }
        }

        if (game != null && game.gameState() == GameState.RUNNING) {
            if (identifier.startsWith("leaderboard_score_")) {
                final int n = Integer.parseInt(identifier.replace("leaderboard_score_", ""));
                final List<GamePlayer> sorted = new ArrayList<>(game.leaderboard().keySet());
                if (n >= sorted.size()) return "N/A";
                return Util.addCommas(game.leaderboard().get(sorted.get(n)));
            } else if (identifier.startsWith("leaderboard_player_")) {
                final int n = Integer.parseInt(identifier.replace("leaderboard_player_", ""));
                final List<GamePlayer> sorted = new ArrayList<>(game.leaderboard().keySet());
                if (n >= sorted.size()) return "N/A";
                return sorted.get(n).player().getName();
            }
        }

        // Game stats only
        if (game != null) {
            GameStats stats = game.getGameData(gp);
            switch (identifier) {
                case "current_score" -> {
                    return Util.addCommas(stats.score());
                }
                case "current_ores_mined" -> {
                    return Util.addCommas(stats.currentOresMined());
                }
            }
        }

        // Data only
        switch (identifier) {
            case "highest_score" -> {
                return Util.addCommas(gp.data().highestScore());
            }
            case "total_ores_mined" -> {
                return Util.addCommas(gp.data().totalOresMined());
            }
            case "wins" -> {
                return Util.addCommas(gp.data().wins());
            }
            case "current_respawns" -> {
                return Integer.toString(gp.data().respawns());
            }
        }

        return switch (identifier) {
            case "x" -> Integer.toString(player.getLocation().getBlockX());
            case "y" -> Integer.toString(player.getLocation().getBlockY());
            case "z" -> Integer.toString(player.getLocation().getBlockZ());
            default -> "";
        };
    }

    private ScoreboardProvider genericScoreboard(String token, Function<Integer, PlayerScoreEntry> scoreEntryFunction) {
        return identifier -> {
            if (identifier.startsWith(token)) {
                final String path = identifier.replace(token, "");
                final String[] tokens = path.split("_");
                final int place = Integer.parseInt(tokens[0]);

                final PlayerScoreEntry winEntry = scoreEntryFunction.apply(place);
                if (winEntry == null) {
                    return "";
                }

                switch (tokens[1]) {
                    case "playername" -> {
                        final PlayerProfile playerProfile = Bukkit.createProfile(winEntry.player());
                        playerProfile.complete();

                        return playerProfile.getName();
                    }
                    case "playeruuid" -> {
                        return winEntry.player().toString();
                    }
                    case "score" -> {
                        return Integer.toString(winEntry.score());
                    }
                    default -> {
                        return "";
                    }
                }
            } else {
                return null;
            }
        };
    }

    private interface ScoreboardProvider extends Function<String, @Nullable String> {

    }

}

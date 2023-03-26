package com.gamersafer.minecraft.abbacaving.placeholders;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.game.PlayerWinEntry;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import com.gamersafer.minecraft.abbacaving.util.Util;
import java.util.ArrayList;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GamePlaceholders extends PlaceholderExpansion {

    private final AbbaCavingPlugin plugin;

    public GamePlaceholders(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
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
        this.plugin.getLogger().info("PlaceholderAPI request: " + player.getName() + ", " + identifier);

        final GamePlayer gp = this.plugin.gameTracker().findPlayerInGame(player);
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

            final LobbyQueue queue = this.plugin.lobby().lobbyQueue(mapName);

            if (queue != null) {
                return switch (suffix) {
                    case "state" -> queue.state().displayName();
                    case "players" -> Integer.toString(queue.playerQueue().size());
                    case "slots" -> Integer.toString(queue.maxPlayers());
                    case "counter" -> Integer.toString(queue.counter());
                    case "required" -> Integer.toString(queue.getStartPlayerAmount());
                    default -> "";
                };
            }

        }
        if (identifier.startsWith("game_")) {
            String path = identifier.replace("game_", "");
            String[] tokens = path.split("_");
            String gameId = tokens[0];
            if (tokens[1].equals("leaderboard")) {
                int place = Integer.parseInt(tokens[2]);
                PlayerWinEntry winEntry = this.plugin.playerDataSource().getWinEntry(gameId, place);
                switch (tokens[2]) {
                    case "playername" -> {
                        PlayerProfile playerProfile = Bukkit.createProfile(winEntry.player());
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


        if (identifier.startsWith("leaderboard_score_")) {
            final int n = Integer.parseInt(identifier.replace("leaderboard_score_", ""));

            if (game.gameState() == GameState.RUNNING) {
                final List<GamePlayer> sorted = new ArrayList<>(game.leaderboard().keySet());
                if (n >= sorted.size()) return "N/A";
                return Util.addCommas(game.leaderboard().get(sorted.get(n)));
            }
            return "";
        } else if (identifier.startsWith("leaderboard_player_")) {
            final int n = Integer.parseInt(identifier.replace("leaderboard_player_", ""));

            if (game.gameState() == GameState.RUNNING) {
                final List<GamePlayer> sorted = new ArrayList<>(game.leaderboard().keySet());
                if (n >= sorted.size()) return "N/A";
                return sorted.get(n).player().getName();
            }
            return "";
        }

        if (gp != null) {
            switch (identifier) {
                case "current_score":
                    return Util.addCommas(gp.gameStats().score());
                case "highest_score":
                    return Util.addCommas(gp.highestScore());
                case "current_ores_mined":
                    return Util.addCommas(gp.gameStats().currentOresMined());
                case "total_ores_mined":
                    return Util.addCommas(gp.totalOresMined());
                case "wins":
                    return Util.addCommas(gp.wins());
                case "game_players":
                    return Integer.toString(game.players().size());
                case "game_maxplayers":
                    return Integer.toString(game.maxPlayersPerRound());
                case "map_name":
                    return game.mapName();
                case "game_id":
                    return game.gameId();
                case "game_state":
                    return game.gameState().displayName();
                case "current_respawns":
                    return Integer.toString(gp.respawns());
            }
        }

        return switch (identifier) {
            case "x" -> Integer.toString(player.getLocation().getBlockX());
            case "y" -> Integer.toString(player.getLocation().getBlockY());
            case "z" -> Integer.toString(player.getLocation().getBlockZ());
            default -> "";
        };
    }

}

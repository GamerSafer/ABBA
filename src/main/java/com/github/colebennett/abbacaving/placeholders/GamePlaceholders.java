package com.github.colebennett.abbacaving.placeholders;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.GamePlayer;
import com.github.colebennett.abbacaving.game.GameState;
import com.github.colebennett.abbacaving.util.Util;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GamePlaceholders extends PlaceholderExpansion {

    private final AbbaCavingPlugin plugin;

    public GamePlaceholders(AbbaCavingPlugin plugin) {
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

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        plugin.getLogger().info("PlaceholderAPI request: " + player.getName() + ", " + identifier);

        GamePlayer gp = plugin.getGame().getPlayer(player);

        if (identifier.startsWith("leaderboard_score_")) {
            int n = Integer.parseInt(identifier.replace("leaderboard_score_", ""));
            if (plugin.getGame().getState() == GameState.RUNNING) {
                List<GamePlayer> sorted = new ArrayList<>(plugin.getGame().getLeaderboard().keySet());
                if (n >= sorted.size()) return "N/A";
                return Util.addCommas(plugin.getGame().getLeaderboard().get(sorted.get(n)));
            }
            return "";
        } else if (identifier.startsWith("leaderboard_player_")) {
            int n = Integer.parseInt(identifier.replace("leaderboard_player_", ""));
            if (plugin.getGame().getState() == GameState.RUNNING) {
                List<GamePlayer> sorted = new ArrayList<>(plugin.getGame().getLeaderboard().keySet());
                if (n >= sorted.size()) return "N/A";
                return sorted.get(n).getPlayer().getName();
            }
            return "";
        }

        switch (identifier) {
            case "current_score":
                if (gp != null) {
                    return Util.addCommas(gp.getScore());
                }
            case "highest_score":
                if (gp != null) {
                    return Util.addCommas(gp.getHighestScore());
                }
            case "current_ores_mined":
                if (gp != null) {
                    return Util.addCommas(gp.getCurrentOresMined());
                }
            case "total_ores_mined":
                if (gp != null) {
                    return Util.addCommas(gp.getTotalOresMined());
                }
            case "wins":
                if (gp != null) {
                    return Util.addCommas(gp.getWins());
                }
            case "game_players":
                if (gp != null) {
                    return Integer.toString(plugin.getGame().getPlayers().size());
                }
            case "game_state":
                if (gp != null) {
                    return plugin.getGame().getState().getDisplayName();
                }
            case "x":
                return Integer.toString(player.getLocation().getBlockX());
            case "y":
                return Integer.toString(player.getLocation().getBlockY());
            case "z":
                return Integer.toString(player.getLocation().getBlockZ());
             default:
                return "";
        }
    }
}

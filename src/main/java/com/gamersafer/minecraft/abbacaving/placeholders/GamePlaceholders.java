package com.gamersafer.minecraft.abbacaving.placeholders;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.util.Util;
import java.util.ArrayList;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
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

        final GamePlayer gp = this.plugin.currentGame().player(player);

        if (identifier.startsWith("leaderboard_score_")) {
            final int n = Integer.parseInt(identifier.replace("leaderboard_score_", ""));

            if (this.plugin.currentGame().gameState() == GameState.RUNNING) {
                final List<GamePlayer> sorted = new ArrayList<>(this.plugin.currentGame().leaderboard().keySet());
                if (n >= sorted.size()) return "N/A";
                return Util.addCommas(this.plugin.currentGame().leaderboard().get(sorted.get(n)));
            }
            return "";
        } else if (identifier.startsWith("leaderboard_player_")) {
            final int n = Integer.parseInt(identifier.replace("leaderboard_player_", ""));

            if (this.plugin.currentGame().gameState() == GameState.RUNNING) {
                final List<GamePlayer> sorted = new ArrayList<>(this.plugin.currentGame().leaderboard().keySet());
                if (n >= sorted.size()) return "N/A";
                return sorted.get(n).player().getName();
            }
            return "";
        }

        if (gp != null) {
            switch (identifier) {
                case "current_score":
                    return Util.addCommas(gp.score());
                case "highest_score":
                    return Util.addCommas(gp.highestScore());
                case "current_ores_mined":
                    return Util.addCommas(gp.currentOresMined());
                case "total_ores_mined":
                    return Util.addCommas(gp.totalOresMined());
                case "wins":
                    return Util.addCommas(gp.wins());
                case "game_players":
                    return Integer.toString(this.plugin.currentGame().players().size());
                case "game_state":
                    return this.plugin.currentGame().gameState().displayName();
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

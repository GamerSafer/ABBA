package com.gamersafer.minecraft.abbacaving.placeholders;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.PlayerWinEntry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class OldGamesPlaceholders extends PlaceholderExpansion {

    private final AbbaCavingPlugin plugin;

    public OldGamesPlaceholders(final AbbaCavingPlugin plugin) {
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

        return "";
    }

}

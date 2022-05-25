package com.github.colebennett.abbacaving.placeholders;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.AbbaCavingPlugin.ServerInfo;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class LobbyPlaceholders extends PlaceholderExpansion {

    private final AbbaCavingPlugin plugin;

    public LobbyPlaceholders(AbbaCavingPlugin plugin) {
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

        switch (identifier) {
            case "online":
                int totalOnline = 0;
                if (plugin.getServers() != null) {
                    for (ServerInfo info : plugin.getServers().values()) {
                        totalOnline += info.playerCount;
                    }
                }
                return Integer.toString(totalOnline);
             default:
                return "";
        }
    }
}

package com.github.colebennett.abbacaving.placeholders;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.AbbaCavingPlugin.ServerInfo;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class LobbyPlaceholders extends PlaceholderExpansion {

    private final AbbaCavingPlugin plugin;

    public LobbyPlaceholders(final AbbaCavingPlugin plugin) {
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
    public String onPlaceholderRequest(final Player player, final String identifier) {
        this.plugin.getLogger().info("PlaceholderAPI request: " + player.getName() + ", " + identifier);

        if ("online".equals(identifier)) {
            int totalOnline = 0;
            if (this.plugin.servers() != null) {
                for (final ServerInfo info : this.plugin.servers().values()) {
                    totalOnline += info.playerCount;
                }
            }
            return Integer.toString(totalOnline);
        }
        return "";
    }

}

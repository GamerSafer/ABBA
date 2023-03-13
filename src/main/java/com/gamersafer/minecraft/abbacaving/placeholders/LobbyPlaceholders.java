package com.gamersafer.minecraft.abbacaving.placeholders;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
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
            return Integer.toString(Bukkit.getServer().getOnlinePlayers().size());
        }

        if (!identifier.startsWith("map_")) {
            return "";
        }

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

        return "";
    }

}

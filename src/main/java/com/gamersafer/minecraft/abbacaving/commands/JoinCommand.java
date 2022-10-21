package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JoinCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public JoinCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final LobbyQueue queue;

        if (args.length == 0 || args[0].equalsIgnoreCase("random")) {
            final List<String> mapNames = this.plugin.configuredMapNames();
            final String mapName = mapNames.get(ThreadLocalRandom.current().nextInt(mapNames.size()));

            queue = this.plugin.lobby().lobbyQueue(mapName);
            // TODO: don't try to put player into active games or full lobbies
            //  add activeQueues() method to Lobby class and pick random from there.
        } else {
            final String input = args[0];

            if (this.plugin.mapSettings(input) == null) {
                this.plugin.message(sender, this.plugin.configMessage("join-invalid-map"));
                return false;
            }

            queue = this.plugin.lobby().lobbyQueue(input);
        }

        final Player player;

        if (args.length == 2) {
            player = Bukkit.getPlayer(args[1]);

            if (player == null) {
                this.plugin.message(sender, this.plugin.configMessage("not-online"), Map.of("player", args[1]));
                return true;
            }
        } else {
            if (sender instanceof Player playerSender) {
                player = playerSender;
            } else {
                return true;
            }
        }

        if (!queue.acceptingNewPlayers() && !player.hasPermission("abbacaving.join.full")) {
            this.plugin.message(sender, this.plugin.configMessage("join-full"));
            return false;
        }

        queue.addPlayer(player.getUniqueId());

        this.plugin.message(sender, this.plugin.configMessage("join-lobby"), Map.of("map", queue.mapName()));

        return true;
    }

}

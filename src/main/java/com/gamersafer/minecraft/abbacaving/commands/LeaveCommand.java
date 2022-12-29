package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LeaveCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public LeaveCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final Player player;

        if (args.length == 1) {
            player = Bukkit.getPlayer(args[0]);

            if (player == null) {
                this.plugin.message(sender, this.plugin.configMessage("not-online"), Map.of("player", args[0]));
                return true;
            }
        } else {
            if (sender instanceof Player playerSender) {
                player = playerSender;
            } else {
                return false;
            }
        }

        final LobbyQueue queue = this.plugin.lobby().lobbyQueue(player);

        if (queue == null) {
            this.plugin.message(sender, this.plugin.configMessage("not-in-queue"));
            return true;
        }

        queue.removePlayer(player.getUniqueId());
        this.plugin.message(player, this.plugin.configMessage("leave-lobby"), Map.of("map", queue.mapName()));

        return true;
    }

}

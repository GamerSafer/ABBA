package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BroadcastNPCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public BroadcastNPCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (sender instanceof Player player) {
            if (!this.plugin.hasPermission(player, "broadcast")) {
                this.plugin.message(sender, "<red>You do not have permission to do this.");
                return false;
            }
        }

        this.plugin.broadcast(String.join(" ", args));
        return true;
    }

}

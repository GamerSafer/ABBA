package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

public class ResetCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public ResetCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Type /reset confirm to confirm deletion of all data! Please make sure all players are offline.", NamedTextColor.RED));
            return true;
        } else if (args[0].equals("confirm")) {
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                sender.sendMessage(Component.text("Server is not empty!", NamedTextColor.RED));
                return true;
            }

            sender.sendMessage(Component.text("Reset all player data!", NamedTextColor.GREEN));
            this.plugin.playerDataSource().purge();
        }
        return true;
    }

}

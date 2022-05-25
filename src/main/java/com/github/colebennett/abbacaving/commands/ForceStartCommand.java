package com.github.colebennett.abbacaving.commands;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ForceStartCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public ForceStartCommand(AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (plugin.getGame().getState() != GameState.WAITING) {
            plugin.message(sender, "<red>Game is not in the waiting state.");
            return false;
        }
        plugin.getGame().preStart();
        return true;
    }
}

package com.github.colebennett.abbacaving.commands;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ForceStartCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public ForceStartCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (this.plugin.currentGame().gameState() != GameState.WAITING) {
            this.plugin.message(sender, "<red>Game is not in the waiting state.");
            return false;
        }
        this.plugin.currentGame().preStart();
        return true;
    }

}

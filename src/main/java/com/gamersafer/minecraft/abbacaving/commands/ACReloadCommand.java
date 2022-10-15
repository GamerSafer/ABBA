package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ACReloadCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public ACReloadCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        this.plugin.reloadConfig();
        this.plugin.message(sender, this.plugin.configMessage("reloaded-config"));
        return true;
    }

}

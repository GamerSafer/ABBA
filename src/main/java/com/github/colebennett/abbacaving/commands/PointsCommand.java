package com.github.colebennett.abbacaving.commands;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.CaveOre;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PointsCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public PointsCommand(AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Set<CaveOre> ores = plugin.getOres();
        if (ores == null) {
            plugin.message(sender, "<red>Not available.");
            return false;
        }

        plugin.message(sender, "<dark_aqua><bold>POINTS");
        for (CaveOre ore : ores) {
            plugin.message(sender, " <gray>" + ore.getName() + ": <green>" + ore.getValue());
        }
        return true;
    }
}

package com.github.colebennett.abbacaving.commands;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.CaveOre;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class PointsCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public PointsCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final Set<CaveOre> ores = this.plugin.ores();
        if (ores == null) {
            this.plugin.message(sender, "<red>Not available.");
            return false;
        }

        this.plugin.message(sender, "<dark_aqua><bold>POINTS");
        for (final CaveOre ore : ores) {
            this.plugin.message(sender, " <gray>" + ore.name() + ": <green>" + ore.value());
        }
        return true;
    }

}

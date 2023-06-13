package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import java.util.Map;
import java.util.Set;

import com.gamersafer.minecraft.abbacaving.util.Messages;
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
        final Set<CaveOre> ores = this.plugin.getLootHandler().getOres();

        if (ores == null) {
            Messages.message(sender, this.plugin.configMessage("points-not-available"));
            return false;
        }

        Messages.message(sender, this.plugin.configMessage("points-points"));

        for (final CaveOre ore : ores) {
            Messages.message(sender, this.plugin.configMessage("points-entry"), Map.of(
                    "ore_name", ore.name(),
                    "points", String.valueOf(ore.value())
            ));
        }

        return true;
    }

}

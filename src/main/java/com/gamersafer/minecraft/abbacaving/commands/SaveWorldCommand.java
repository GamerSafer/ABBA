package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class SaveWorldCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public SaveWorldCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        String currentWorld = player.getWorld().getName();
        Set<String> worldsToSave = plugin.getWorldsToSave();
        if (worldsToSave.contains(currentWorld)) {
            sender.sendMessage(Component.text("The world you are currently in will no longer be saved."));
            worldsToSave.remove(currentWorld);
        } else {
            sender.sendMessage(Component.text("The world you are currently in will now be saved."));
            worldsToSave.add(currentWorld);
        }

        return true;
    }

}

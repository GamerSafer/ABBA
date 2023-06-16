package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ForceSaveWorldCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public ForceSaveWorldCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        String currentWorld = player.getWorld().getName();
        Set<String> worldsToSave = plugin.getWorldsToSave();
        worldsToSave.add(currentWorld);
        sender.sendMessage(Component.text("Force saving world..."));
        player.getWorld().save();
        sender.sendMessage(Component.text("Saved!"));
        worldsToSave.remove(currentWorld);

        return true;
    }

}

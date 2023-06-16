package com.gamersafer.minecraft.abbacaving.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import com.gamersafer.minecraft.abbacaving.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DeleteDataCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public DeleteDataCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        PlayerProfile playerProfile = Bukkit.createProfile(args[0]);
        playerProfile.complete();
        playerProfile.update().thenAccept((profile) -> {
            UUID uuid = profile.getId();

            this.plugin.playerDataSource().delete(uuid);
            sender.sendMessage(Component.text(profile.getName() + "'s data has been deleted...", NamedTextColor.GRAY));
        });


        return true;
    }

}

package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DebugCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public DebugCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        for (Game game : this.plugin.gameTracker().currentGames()) {
            System.out.println(game);
        }

        return true;
    }

    private boolean isBlockSafe(final Block block) {
        return block.isEmpty() || block.isPassable() && !block.isLiquid();
    }

}

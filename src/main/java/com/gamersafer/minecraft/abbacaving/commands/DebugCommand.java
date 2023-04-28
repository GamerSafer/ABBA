package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
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

        final Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (args.length == 1) {
            System.out.println(block);
            System.out.println(block.isSolid());
            System.out.println(this.isBlockSafe(block.getRelative(BlockFace.UP)));
            System.out.println(this.isBlockSafe(block.getRelative(BlockFace.UP, 2)));
        } else {
            this.plugin.gameTracker().gamePlayer(player).gameStats().game().randomLocation(player).thenAccept(loc -> {
                player.teleport(loc.toCenterLocation());
            });
        }

        return true;
    }

    private boolean isBlockSafe(final Block block) {
        return block.isEmpty() || block.isPassable() && !block.isLiquid();
    }

}

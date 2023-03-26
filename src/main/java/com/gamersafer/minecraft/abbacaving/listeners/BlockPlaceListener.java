package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public BlockPlaceListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent event) {
        final boolean canBuild = event.getPlayer().hasPermission("abbacaving.build");

        if (event.getPlayer().getWorld().getName().equals("world")) {
            if (!canBuild) {
                event.setCancelled(true);
            }
            return;
        }

        final Game game = this.plugin.gameTracker().findGame(event.getBlock().getWorld());

        if (game == null) {
            if (!canBuild) {
                event.setCancelled(true);
            }
            return;
        }

        if (game.gameState() != GameState.RUNNING) {
            if (!canBuild) {
                event.setCancelled(true);
            }
            return;
        }

        final ItemStack item = event.getItemInHand();
        SlottedHotbarTool toolType = SlottedHotbarTool.getStored(item);
        if (toolType != null && toolType.isInfinite()) {
            item.setAmount(1);
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == item.getType()) {
                event.getPlayer().getInventory().setItemInMainHand(item);
            } else if (event.getPlayer().getInventory().getItemInOffHand().getType() == item.getType()) {
                event.getPlayer().getInventory().setItemInOffHand(item);
            }
        }
    }

}

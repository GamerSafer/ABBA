package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class BlockPlaceListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public BlockPlaceListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLiquidPlace(PlayerBucketEmptyEvent event) {
        final boolean canBuild = event.getPlayer().hasPermission("abbacaving.build");

        final Game game = this.plugin.gameTracker().getGame(event.getBlock().getWorld());

        if (game == null) {
            if (!canBuild) {
                event.setCancelled(true);
            }
        }
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

        final Game game = this.plugin.gameTracker().getGame(event.getBlock().getWorld());

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
        final SlottedHotbarTool toolType = SlottedHotbarTool.stored(item);
        if (toolType != null && toolType.isInfinite()) {
            item.setAmount(1);
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == item.getType()) {
                event.getPlayer().getInventory().setItemInMainHand(item);
            } else if (event.getPlayer().getInventory().getItemInOffHand().getType() == item.getType()) {
                event.getPlayer().getInventory().setItemInOffHand(item);
            }
        }
    }

    @EventHandler
    public void onCustomBlockPlace(final CustomBlockPlaceEvent event) {
        final ItemStack item = event.getItemInHand();
        final SlottedHotbarTool toolType = SlottedHotbarTool.stored(item);
        if (toolType != null && toolType.isInfinite()) {
            item.setAmount(1);
            final ItemStack mainItem = event.getPlayer().getInventory().getItemInMainHand();
            final ItemStack offhandItem = event.getPlayer().getInventory().getItemInOffHand();
            if (mainItem.getType() == item.getType() || mainItem.getType().isAir()) {
                event.getPlayer().getInventory().setItemInMainHand(item);
            } else if (offhandItem.getType() == item.getType() || offhandItem.getType().isAir()) {
                event.getPlayer().getInventory().setItemInOffHand(item);
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getPlayer().updateInventory();
                }
            }.runTaskLater(this.plugin, 1);
        }
    }

}

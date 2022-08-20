package com.github.colebennett.abbacaving.listeners;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.GameState;
import org.bukkit.Location;
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
        if (event.getPlayer().getWorld().getName().equals("world")) {
            event.setCancelled(true);
            return;
        }

        if (this.plugin.currentGame().gameState() != GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }

        final Location loc = event.getBlock().getLocation();
        for (final Location spawn : this.plugin.currentGame().spawnLocations()) {
            if (!this.plugin.canAccess(loc, spawn)) {
                event.setCancelled(true);
                this.plugin.message(event.getPlayer(), this.plugin.configMessage("cannot-mine-near-spawn"));
                return;
            }
        }

        final ItemStack item = event.getItemInHand();
        if (item.getType() == Material.TORCH || item.getType() == Material.CRAFTING_TABLE) {
            item.setAmount(1);
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == item.getType()) {
                event.getPlayer().getInventory().setItemInMainHand(item);
            } else if (event.getPlayer().getInventory().getItemInOffHand().getType() == item.getType()) {
                event.getPlayer().getInventory().setItemInOffHand(item);
            }
        }
    }

}

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

    public BlockPlaceListener(AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().getWorld().getName().equals("world")) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getGame().getState() != GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }

        Location loc = event.getBlock().getLocation();
        for (Location spawn : plugin.getGame().getSpawns()) {
            if (!plugin.canAccess(loc, spawn)) {
                event.setCancelled(true);
                plugin.message(event.getPlayer(), plugin.getMessage("cannot-mine-near-spawn"));
                return;
            }
        }

        ItemStack item = event.getItemInHand();
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

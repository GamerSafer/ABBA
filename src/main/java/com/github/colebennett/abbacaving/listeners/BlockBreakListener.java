package com.github.colebennett.abbacaving.listeners;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.CaveOre;
import com.github.colebennett.abbacaving.game.GamePlayer;
import com.github.colebennett.abbacaving.game.GameState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class BlockBreakListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public BlockBreakListener(AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
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

        CaveOre ore = plugin.caveOreFromBlock(event.getBlock().getType());
        if (ore != null) {
            Player player = event.getPlayer();
            GamePlayer gp = plugin.getGame().getPlayer(player);

            if (ore.getValue() > 0) {
                gp.setCurrentOresMined(gp.getCurrentOresMined() + 1);
                gp.setTotalOresMined(gp.getTotalOresMined() + 1);
                gp.addScore(ore.getValue(), ore.getName());
            } else {
                plugin.message(player, plugin.getMessage("ore-not-worth-points"), new HashMap<String, String>(){{
                    put("ore", ore.getName());
                }});
            }
        }

        if (event.getBlock().getType() == Material.STONE) {
            if (!hasMaxAmount(event.getPlayer().getInventory(), Material.COBBLESTONE)) {
                event.getPlayer().getInventory().addItem(new ItemStack(Material.COBBLESTONE));
            } else {
//                plugin.message(event.getPlayer(), "<gray>You already have the maximum allowed amount of cobblestone (2 stacks).");
            }
        }

        event.setExpToDrop(0);
        event.setDropItems(false);
    }

    private boolean hasMaxAmount(Inventory inv, Material type) {
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == type) {
                count += item.getAmount();
            }
        }
        return count >= 128;
    }
}

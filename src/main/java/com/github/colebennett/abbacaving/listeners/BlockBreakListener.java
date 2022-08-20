package com.github.colebennett.abbacaving.listeners;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.CaveOre;
import com.github.colebennett.abbacaving.game.GamePlayer;
import com.github.colebennett.abbacaving.game.GameState;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BlockBreakListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public BlockBreakListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
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

        final CaveOre ore = this.plugin.caveOreFromBlock(event.getBlock().getType());
        if (ore != null) {
            final Player player = event.getPlayer();
            final GamePlayer gp = this.plugin.currentGame().player(player);

            if (ore.value() > 0) {
                gp.currentOresMined(gp.currentOresMined() + 1);
                gp.totalOresMined(gp.totalOresMined() + 1);
                gp.addScore(ore.value(), ore.name());
            } else {
                this.plugin.message(player, this.plugin.configMessage("ore-not-worth-points"), new HashMap<>() {{
                            this.put("ore", ore.name());
                        }}
                );
            }
        }

        if (event.getBlock().getType() == Material.STONE) {
            if (!this.hasMaxAmount(event.getPlayer().getInventory(), Material.COBBLESTONE)) {
                event.getPlayer().getInventory().addItem(new ItemStack(Material.COBBLESTONE));
            } else {
                //plugin.message(event.getPlayer(), "<gray>You already have the maximum allowed amount of cobblestone (2 stacks).");
            }
        }

        event.setExpToDrop(0);
        event.setDropItems(false);
    }

    private boolean hasMaxAmount(final Inventory inv, final Material type) {
        int count = 0;
        for (final ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == type) {
                count += item.getAmount();
            }
        }
        return count >= 128;
    }

}

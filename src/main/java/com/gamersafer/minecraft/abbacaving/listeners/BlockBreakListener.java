package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

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

        final Game game = this.plugin.gameTracker().findGame(event.getBlock().getWorld());

        if (game == null || game.gameState() != GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }

        final Location loc = event.getBlock().getLocation();

        for (final Location spawn : game.spawnLocations()) {
            if (!game.canAccess(loc, spawn)) {
                event.setCancelled(true);
                this.plugin.message(event.getPlayer(), this.plugin.configMessage("cannot-mine-near-spawn"));
                return;
            }
        }

        final CaveOre ore = this.plugin.caveOreFromBlock(event.getBlock().getType());
        if (ore != null) {
            final Player player = event.getPlayer();
            final GamePlayer gp = this.plugin.gameTracker().findPlayer(player);

            if (gp == null) {
                return;
            }

            if (ore.value() > 0) {
                gp.currentOresMined(gp.currentOresMined() + 1);
                gp.totalOresMined(gp.totalOresMined() + 1);
                gp.addScore(ore.value(), ore.name());
                game.increasePlayerScore(gp, ore.value());
            } else {
                this.plugin.message(player, this.plugin.configMessage("ore-not-worth-points"),
                        TagResolver.resolver("ore", Tag.preProcessParsed(ore.name())));
            }
        }

        event.setExpToDrop(0);
        event.setDropItems(false);
    }

}

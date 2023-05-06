package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.util.Sounds;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
        final boolean canBuild = event.getPlayer().hasPermission("abbacaving.build");

        if (event.getPlayer().getWorld().getName().equals("world")) {
            if (!canBuild) {
                event.setCancelled(true);
            }
            return;
        }

        final Game game = this.plugin.gameTracker().findGame(event.getBlock().getWorld());

        if (game == null || game.gameState() != GameState.RUNNING) {
            if (!canBuild) {
                event.setCancelled(true);
            }
            return;
        }

        final CaveOre ore = this.plugin.caveOreFromBlock(event.getBlock().getType());
        if (ore != null) {
            final Player player = event.getPlayer();
            final GamePlayer gp = this.plugin.gameTracker().findPlayerInGame(player);

            if (gp == null) {
                return;
            }

            if (ore.value() > 0) {
                gp.gameStats().currentOresMined(gp.gameStats().currentOresMined() + 1);
                gp.data().incrementMinedOres();
                gp.gameStats().addScore(ore.value(), ore.name());
                game.increasePlayerScore(gp, ore.value());
                Sounds.pling(player);
            } else {
                this.plugin.message(player, this.plugin.configMessage("ore-not-worth-points"),
                        TagResolver.resolver("ore", Tag.preProcessParsed(ore.name())));
            }
        }

        event.setExpToDrop(0);
        event.setDropItems(false);
    }

}

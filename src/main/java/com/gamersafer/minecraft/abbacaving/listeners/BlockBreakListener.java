package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.player.GameStats;
import com.gamersafer.minecraft.abbacaving.util.Messages;
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

        final Game game = this.plugin.gameTracker().getGame(event.getBlock().getWorld());

        if (game == null || game.gameState() != GameState.RUNNING) {
            if (!canBuild) {
                event.setCancelled(true);
            }
            return;
        }

        final CaveOre ore = this.plugin.getLootHandler().caveOreFromBlock(event.getBlock().getType());
        if (ore != null) {
            final Player player = event.getPlayer();
            GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player);

            GameStats stats = game.getGameData(gamePlayer);
            if (ore.value() > 0) {
                stats.currentOresMined(stats.currentOresMined() + 1);
                gamePlayer.data().incrementMinedOres();
                stats.addScore(ore.value(), ore.name());
                game.increasePlayerScore(gamePlayer, ore.value());
                Sounds.pling(player);
            } else {
                Messages.message(player, this.plugin.configMessage("ore-not-worth-points"),
                        TagResolver.resolver("ore", Tag.preProcessParsed(ore.name())));
            }
        }

        event.setExpToDrop(0);
        event.setDropItems(false);
    }

}

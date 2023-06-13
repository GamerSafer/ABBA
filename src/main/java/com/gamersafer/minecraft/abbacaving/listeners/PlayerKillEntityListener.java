package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.util.Sounds;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class PlayerKillEntityListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public PlayerKillEntityListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerKillEntity(final EntityDeathEvent event) {
        final Player player = event.getEntity().getKiller();
        if (player == null) {
            return;
        }

        Game game = this.plugin.gameTracker().findGame(player);
        if (game != null && game.countMobKills()) {
            GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player.getUniqueId());
            game.getGameData(gamePlayer).addScore(1, "Mob Kill");
            game.increasePlayerScore(gamePlayer, 1);
            Sounds.pling(player);
        }
    }

}

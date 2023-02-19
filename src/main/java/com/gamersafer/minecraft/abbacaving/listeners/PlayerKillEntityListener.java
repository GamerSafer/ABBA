package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.util.Sounds;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class PlayerKillEntityListener implements Listener {

    private final AbbaCavingPlugin cavingPlugin;

    public PlayerKillEntityListener(AbbaCavingPlugin cavingPlugin) {
        this.cavingPlugin = cavingPlugin;
    }

    @EventHandler
    public void playerKillEntity(EntityDeathEvent event) {
        final Player player = event.getEntity().getKiller();
        if (player == null) {
            return;
        }

        final GamePlayer gp = this.cavingPlugin.gameTracker().findPlayerInGame(player);

        if (gp == null) {
            return;
        }

        Game game = gp.gameStats().game();
        if (game.countMobKills()) {
            gp.gameStats().addScore(1, "Mob Kill");
            game.increasePlayerScore(gp, 1);
            Sounds.pling(player);
        }
    }
}

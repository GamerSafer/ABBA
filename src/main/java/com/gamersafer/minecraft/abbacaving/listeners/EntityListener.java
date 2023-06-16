package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

public class EntityListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public EntityListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemSpawn(final ItemSpawnEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.DROPPED_ITEM) {
            return;
        }
        Game game = this.plugin.gameTracker().getGame(event.getEntity().getWorld());
        if (game == null) {
            event.setCancelled(true);
        }
    }

}

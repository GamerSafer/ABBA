package com.github.colebennett.abbacaving.listeners;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.GameState;
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
        if (event.getEntityType() != EntityType.DROPPED_ITEM
                && this.plugin.currentGame().gameState() != GameState.RUNNING) {
            event.setCancelled(true);
        }
    }

}

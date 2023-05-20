package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.HashSet;
import java.util.Set;

public class WorldListener implements Listener {

    private final Set<String> worlds;
    private final AbbaCavingPlugin plugin;

    public WorldListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
        this.worlds = new HashSet<>(this.plugin.getConfig().getStringList("save-worlds"));
    }

    @EventHandler
    public void saveEvent(ChunkUnloadEvent event) {
        if (!worlds.contains(event.getWorld().getName())) {
            event.setSaveChunk(false);
        }
    }
}

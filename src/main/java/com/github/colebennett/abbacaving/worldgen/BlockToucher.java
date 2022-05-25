package com.github.colebennett.abbacaving.worldgen;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Queue;

public class BlockToucher {

    private static final BlockFace[] faces = {
            BlockFace.SELF,
            BlockFace.UP,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    };

    private static final int TOUCHES_PER_TICK = 50;

    private final Plugin plugin;
    private final Queue<Block> needsTouching = new ArrayDeque<>();
    private boolean running;

    BlockToucher(Plugin plugin) {
        this.plugin = plugin;
    }

    public Queue<Block> getQueue() {
        return needsTouching;
    }

    public void touch(Block block) {
        needsTouching.add(block);

        if (!running && plugin.isEnabled()) {
            running = true;
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new TouchTask());
        }
    }

    private class TouchTask implements Runnable {
        @Override
        public void run() {
            if (needsTouching.isEmpty()) {
                running = false;
                return;
            }

            for (int i = 0; i < TOUCHES_PER_TICK; i++) {
                if (!needsTouching.isEmpty()) {
                    Block block = needsTouching.remove();
                    for (BlockFace face : faces) {
                        block.getRelative(face).getState().update(true, true);
                    }
                }
            }

            if (plugin.isEnabled()) {
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, this);
            }
        }
    }
}

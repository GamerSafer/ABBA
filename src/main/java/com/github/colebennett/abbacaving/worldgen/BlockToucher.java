package com.github.colebennett.abbacaving.worldgen;

import java.util.ArrayDeque;
import java.util.Queue;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;

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

    BlockToucher(final Plugin plugin) {
        this.plugin = plugin;
    }

    public Queue<Block> blockQueue() {
        return this.needsTouching;
    }

    public void touch(final Block block) {
        this.needsTouching.add(block);

        if (!this.running && this.plugin.isEnabled()) {
            this.running = true;
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new TouchTask());
        }
    }

    private class TouchTask implements Runnable {
        @Override
        public void run() {
            if (BlockToucher.this.needsTouching.isEmpty()) {
                BlockToucher.this.running = false;
                return;
            }

            for (int i = 0; i < TOUCHES_PER_TICK; i++) {
                if (!BlockToucher.this.needsTouching.isEmpty()) {
                    final Block block = BlockToucher.this.needsTouching.remove();
                    for (final BlockFace face : faces) {
                        block.getRelative(face).getState().update(true, true);
                    }
                }
            }

            if (BlockToucher.this.plugin.isEnabled()) {
                BlockToucher.this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(BlockToucher.this.plugin, this);
            }
        }
    }

}

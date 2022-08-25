package com.github.colebennett.abbacaving.worldgen;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.generator.BlockPopulator;

public class GiantCavePopulator extends BlockPopulator implements Listener {

    // Frequency
    private final double sxz;
    private final double sy;

    // Density
    private final int cutoff;

    // Position
    private final int caveBandMin; // miny
    private final int caveBandMax; // maxy

    private final BlockFace[] faces = {
        BlockFace.UP,
        BlockFace.NORTH,
        BlockFace.SOUTH,
        BlockFace.EAST,
        BlockFace.WEST
    };

    private final AbbaCavingPlugin plugin;
    private final World world;
    private final Material material;
    private final BlockToucher toucher;

    private final List<Chunk> chunks = new ArrayList<>();
    private final List<Location> spawnCandidates = new ArrayList<>();
    int chunkId = 0;
    boolean stop = false;

    public GiantCavePopulator(final AbbaCavingPlugin plugin, final World world) {
        this.plugin = plugin;
        this.world = world;

        this.sxz = plugin.getConfig().getDouble("cave-generator.sxz");
        this.sy = plugin.getConfig().getDouble("cave-generator.sy");
        this.cutoff = plugin.getConfig().getInt("cave-generator.cutoff");
        this.caveBandMin = plugin.getConfig().getInt("cave-generator.miny");
        this.caveBandMax = plugin.getConfig().getInt("cave-generator.maxy");

        plugin.getLogger().info("sxz: " + this.sxz);
        plugin.getLogger().info("sy: " + this.sy);
        plugin.getLogger().info("cutoff: " + this.cutoff);
        plugin.getLogger().info("caveBandMin: " + this.caveBandMin);
        plugin.getLogger().info("caveBandMax: " + this.caveBandMax);

        this.material = Material.AIR;
        this.toucher = new BlockToucher(plugin);
    }

    @Override
    public void populate(final World world, final Random random, final Chunk source) {
        if (this.stop) return;

        final GiantCaveRandom gcRandom = new GiantCaveRandom(source, this.caveBandMin, this.caveBandMax, this.sxz, this.sy, this.cutoff);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = this.caveBandMax; y >= this.caveBandMin; y--) {
                    if (gcRandom.isInGiantCave(x, y, z)) {
                        final Block block = source.getBlock(x, y, z);
                        final Block blockUp1 = block.getRelative(BlockFace.UP);
                        final Block blockUp2 = blockUp1.getRelative(BlockFace.UP);
                        final Block blockUp3 = blockUp2.getRelative(BlockFace.UP);

                        if (this.isHoldingBackOcean(block) || this.isHoldingBackOcean(blockUp1)) {
                            continue;
                        }

                        if (this.isHoldingBackOcean(blockUp2) || this.isHoldingBackOcean(blockUp3)) {
                            // Support the ocean with stone to keep the bottom from falling out.
                            if (block.getType().hasGravity()) { // sand or gravel
                                block.setType(Material.STONE, false);
                                blockUp1.setType(Material.STONE, false);
                            }
                        } else {
                            block.setType(this.material, false);
                            this.toucher.touch(block);

                            //if (random.nextDouble() < 0.05) {
                            //    blockUp1.setType(Material.NETHER_GOLD_ORE);
                            //    toucher.touch(blockUp1);
                            //}
                        }
                    }
                }
            }
        }

        int perChunk = 0;
        this.chunkId++;

        if (this.chunks.size() < 1000 && this.chunkId % 5 == 0) {
            for (int x = 4; x < 12; x++) {
                for (int z = 4; z < 12; z++) {
                    for (int y = this.caveBandMin; y <= this.caveBandMax / 2; y++) {
                        final Block block = source.getBlock(x, y, z);
                        if (block.getY() >= block.getWorld().getHighestBlockYAt(block.getLocation())) {
                            continue;
                        }

                        final Block down = block.getRelative(BlockFace.DOWN);
                        if (block.getType() == Material.AIR && down.getType().isSolid()) {
                            boolean ok = true;
                            for (final BlockFace face : this.faces) {
                                if (block.getRelative(face).getType() != Material.AIR) {
                                    ok = false;
                                    break;
                                }
                            }

                            if (ok) {
                                this.spawnCandidates.add(block.getLocation());
                                if (++perChunk == 20) {
                                    return;
                                }
                                break;
                            }
                        }
                    }
                }
            }
            this.chunks.add(source);
        }

        if (this.chunkId % 500 == 0) {
            this.plugin.getLogger().info(this.chunks.size() + " chunks, " + this.spawnCandidates.size() + " spawn candidates");
        }
    }

    private void findSpawns() {
        this.stop = true;
        this.plugin.getLogger().info("Finding spawns...");
        final int minSpawns = this.plugin.getConfig().getInt("cave-generator.spawns");
        final int spacing = this.plugin.getConfig().getInt("cave-generator.spawn-spacing");
        final List<Location> spawns = new ArrayList<>();

        Collections.shuffle(this.spawnCandidates);
        for (final Location candidate : this.spawnCandidates) {
            boolean farEnough = true;
            for (final Location spawn : spawns) {
                if (spawn.distance(candidate) < spacing) {
                    farEnough = false;
                    break;
                }
            }

            if (!farEnough) continue;

            final int bx = candidate.getBlockX();
            final int by = candidate.getBlockY();
            final int bz = candidate.getBlockZ();

            final int checkDistance = 25;
            final Block[] checks = new Block[] {
                candidate.getBlock().getRelative(bx + checkDistance, by, bz),
                candidate.getBlock().getRelative(bx - checkDistance, by, bz),
                candidate.getBlock().getRelative(bx, by, bz + checkDistance),
                candidate.getBlock().getRelative(bx, by, bz - checkDistance),
            };

            boolean inOpenArea = true;
            for (final Block b : checks) {
                if (b.getType() != Material.AIR) {
                    inOpenArea = false;
                    break;
                }
            }

            if (!inOpenArea) {
                continue;
            }

            spawns.add(candidate);
            this.plugin.getLogger().info("cave spawn location : " + candidate + " (total: " + spawns.size() + ")");
            if (spawns.size() == minSpawns) {
                break;
            }
        }

        this.saveConfig(spawns);
    }

    private boolean isHoldingBackOcean(final Block block) {
        return this.isSurfaceWater(block) || this.isNextToSurfaceWater(block);
    }

    private boolean isNextToSurfaceWater(final Block block) {
        for (final BlockFace face : this.faces) {
            final Block adjacent = block.getRelative(face);
            // Don't look at neighboring chunks to prevent runaway chunk generation.
            // Use block coordinates to compute chunk coordinates to prevent loading chunks.
            if (block.getX() >> 4 == adjacent.getX() >> 4 && block.getZ() >> 4 == adjacent.getZ() >> 4) {
                if (this.isSurfaceWater(adjacent)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSurfaceWater(Block block) {
        // Walk the column of blocks above block looking sea level.
        while (this.isWater(block)) {
            if (block.getY() >= block.getWorld().getSeaLevel() - 1) {
                return true;
            }
            block = block.getRelative(BlockFace.UP);
        }
        return false;
    }

    private boolean isWater(final Block block) {
        final Material material = block.getType();
        return material == Material.WATER ||
                material == Material.ICE ||
                material == Material.BLUE_ICE ||
                material == Material.PACKED_ICE;
    }

    private void saveConfig(final List<Location> spawns) {
        final FileConfiguration config = new YamlConfiguration();
        final File file = new File(this.plugin.getDataFolder(), "spawns.yml");
        if (file.exists()) {
            try {
                config.load(file);
                this.plugin.getLogger().info("Loaded existing file: " + file.getAbsolutePath());
            } catch (final IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        final List<String> spawnList = new ArrayList<>();
        for (final Location spawn : spawns) {
            spawnList.add(String.format("%d,%d,%d",
                    spawn.getBlockX(),
                    spawn.getBlockY(),
                    spawn.getBlockZ()));
        }
        config.set(this.world.getName(), spawnList);

        try {
            config.save(file);
            this.plugin.getLogger().info("Saved " + spawns.size() + " spawn locations for " + this.world.getName() + " to " + file.getAbsolutePath());
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}

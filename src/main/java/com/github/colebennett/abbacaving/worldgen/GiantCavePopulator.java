package com.github.colebennett.abbacaving.worldgen;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.wimbli.WorldBorder.Events.WorldBorderFillFinishedEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.generator.BlockPopulator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    public GiantCavePopulator(AbbaCavingPlugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;

        sxz = plugin.getConfig().getDouble("cave-generator.sxz");
        sy = plugin.getConfig().getDouble("cave-generator.sy");
        cutoff = plugin.getConfig().getInt("cave-generator.cutoff");
        caveBandMin = plugin.getConfig().getInt("cave-generator.miny");
        caveBandMax = plugin.getConfig().getInt("cave-generator.maxy");

        plugin.getLogger().info("sxz: " + sxz);
        plugin.getLogger().info("sy: " + sy);
        plugin.getLogger().info("cutoff: " + cutoff);
        plugin.getLogger().info("caveBandMin: " + caveBandMin);
        plugin.getLogger().info("caveBandMax: " + caveBandMax);

        material = Material.AIR;
        toucher = new BlockToucher(plugin);
    }

    int chunkId = 0;

    @Override
    public void populate(World world, Random random, Chunk source) {
        if (stop)return;

        GiantCaveRandom gcRandom = new GiantCaveRandom(source, caveBandMin, caveBandMax, sxz, sy, cutoff);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = caveBandMax; y >= caveBandMin; y--) {
                    if (gcRandom.isInGiantCave(x, y, z)) {
                        Block block = source.getBlock(x, y, z);
                        Block blockUp1 = block.getRelative(BlockFace.UP);
                        Block blockUp2 = blockUp1.getRelative(BlockFace.UP);
                        Block blockUp3 = blockUp2.getRelative(BlockFace.UP);

                        if (isHoldingBackOcean(block) || isHoldingBackOcean(blockUp1)) {
                            continue;
                        }

                        if (isHoldingBackOcean(blockUp2) || isHoldingBackOcean(blockUp3)) {
                            // Support the ocean with stone to keep the bottom from falling out.
                            if (block.getType().hasGravity()) { // sand or gravel
                                block.setType(Material.STONE, false);
                                blockUp1.setType(Material.STONE, false);
                            }
                        } else {
                            block.setType(material, false);
                            toucher.touch(block);

//                            if (random.nextDouble() < 0.05) {
//                                blockUp1.setType(Material.NETHER_GOLD_ORE);
//                                toucher.touch(blockUp1);
//                            }
                        }
                    }
                }
            }
        }

        int perChunk = 0;
        chunkId++;

        if (chunks.size() < 1000 && chunkId % 5 == 0) {
            for (int x = 4; x < 12; x++) {
                for (int z = 4; z < 12; z++) {
                    for (int y = caveBandMin; y <= caveBandMax / 2; y++) {
                        Block block = source.getBlock(x, y, z);
                        if (block.getY() >= block.getWorld().getHighestBlockYAt(block.getLocation())) {
                            continue;
                        }

                        Block down = block.getRelative(BlockFace.DOWN);
                        if (block.getType() == Material.AIR && down.getType().isSolid()) {
                            boolean ok = true;
                            for (BlockFace face : faces) {
                                if (block.getRelative(face).getType() != Material.AIR) {
                                    ok = false;
                                    break;
                                }
                            }

                            if (ok) {
                                spawnCandidates.add(block.getLocation());
                                if (++perChunk == 20) {
                                    return;
                                }
                                break;
                            }
                        }
                    }
                }
            }
            chunks.add(source);
        }

        if (chunkId % 500 == 0) {
            plugin.getLogger().info(chunks.size() + " chunks, " + spawnCandidates.size() + " spawn candidates");
        }
    }

    boolean stop = false;

    private void findSpawns() {
        stop=true;
        plugin.getLogger().info("Finding spawns...");
        int minSpawns = plugin.getConfig().getInt("cave-generator.spawns");
        int spacing = plugin.getConfig().getInt("cave-generator.spawn-spacing");
        List<Location> spawns = new ArrayList<>();

        Collections.shuffle(spawnCandidates);
        for (Location candidate : spawnCandidates) {
            boolean farEnough = true;
            for (Location spawn : spawns) {
                if (spawn.distance(candidate) < spacing) {
                    farEnough = false;
                    break;
                }
            }

            if (!farEnough) continue;

            int bx = candidate.getBlockX();
            int by = candidate.getBlockY();
            int bz = candidate.getBlockZ();

            int checkDistance = 25;
            Block[] checks = new Block[] {
                    candidate.getBlock().getRelative(bx + checkDistance, by, bz),
                    candidate.getBlock().getRelative(bx - checkDistance, by, bz),
                    candidate.getBlock().getRelative(bx, by, bz + checkDistance),
                    candidate.getBlock().getRelative(bx, by, bz - checkDistance),
            };

            boolean inOpenArea = true;
            for (Block b : checks) {
                if (b.getType() != Material.AIR) {
                    inOpenArea = false;
                    break;
                }
            }

            if (!inOpenArea) {
                continue;
            }

            spawns.add(candidate);
            plugin.getLogger().info("cave spawn location : " + candidate + " (total: " + spawns.size() + ")");
            if (spawns.size() == minSpawns) {
                break;
            }
        }

        saveConfig(spawns);
    }

    private boolean isHoldingBackOcean(Block block) {
        return isSurfaceWater(block) || isNextToSurfaceWater(block);
    }

    private boolean isNextToSurfaceWater(Block block) {
        for (BlockFace face : faces) {
            Block adjacent = block.getRelative(face);
            // Don't look at neighboring chunks to prevent runaway chunk generation.
            // Use block coordinates to compute chunk coordinates to prevent loading chunks.
            if (block.getX() >> 4 == adjacent.getX() >> 4 && block.getZ() >> 4 == adjacent.getZ() >> 4) {
                if (isSurfaceWater(adjacent)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSurfaceWater(Block block) {
        // Walk the column of blocks above block looking sea level.
        while (isWater(block)) {
            if (block.getY() >= block.getWorld().getSeaLevel() - 1) {
                return true;
            }
            block = block.getRelative(BlockFace.UP);
        }
        return false;
    }

    private boolean isWater(Block block) {
        Material material = block.getType();
        return material == Material.WATER ||
                material == Material.ICE ||
                material == Material.BLUE_ICE ||
                material == Material.PACKED_ICE;
    }

    private void saveConfig(List<Location> spawns) {
        FileConfiguration config = new YamlConfiguration();
        File file = new File(plugin.getDataFolder(), "spawns.yml");
        if (file.exists()) {
            try {
                config.load(file);
                plugin.getLogger().info("Loaded existing file: " + file.getAbsolutePath());
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        List<String> spawnList = new ArrayList<>();
        for (Location spawn : spawns) {
            spawnList.add(String.format("%d,%d,%d",
                    spawn.getBlockX(),
                    spawn.getBlockY(),
                    spawn.getBlockZ()));
        }
        config.set(world.getName(), spawnList);

        try {
            config.save(file);
            plugin.getLogger().info("Saved " + spawns.size() + " spawn locations for " + world.getName() + " to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onWorldBorderFillFinished(WorldBorderFillFinishedEvent event) {
        findSpawns();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getServer().shutdown(), 20 * 5);
    }
}

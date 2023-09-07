package com.gamersafer.minecraft.abbacaving.game.map;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.validators.AbbaValidator;
import de.themoep.randomteleport.RandomTeleport;
import de.themoep.randomteleport.ValidatorRegistry;
import de.themoep.randomteleport.searcher.RandomSearcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameMap {

    private final String name;
    private final Logger logger;

    private final AbbaValidator blockValidator;
    private final ConfigurationSection mapSettings;
    private final ConfigurationSection fallbackMapSettings;
    private World world;

    private final AbbaCavingPlugin plugin = AbbaCavingPlugin.getPlugin(AbbaCavingPlugin.class);

    private static final ExecutorService WORLD_LOADER = Executors.newSingleThreadExecutor();

    public GameMap(String mapName, ConfigurationSection mapSettings, Logger logger) {
        this.name = mapName;
        this.logger = logger;
        this.fallbackMapSettings = mapSettings.getConfigurationSection("default-settings");
        this.mapSettings = Objects.requireNonNullElse(mapSettings.getConfigurationSection(mapName), this.fallbackMapSettings);

        this.world = this.loadMap(mapName, false).join();
        this.blockValidator = new AbbaValidator(logger, mapSetting("random-teleport.invalid-blocks"));
    }

    public CompletableFuture<Location> randomLocation(final Player player) {
        final Plugin randomTPPlugin = Bukkit.getPluginManager().getPlugin("RandomTeleport");

        if (randomTPPlugin instanceof RandomTeleport randomTeleport) {
            final int minRadius = this.mapSetting("random-teleport.min-radius");
            final int maxRadius = this.mapSetting("random-teleport.max-radius");

            final int minY = this.mapSetting("random-teleport.min-y");
            final int maxY = this.mapSetting("random-teleport.max-y");

            final int maxTries = this.mapSetting("random-teleport.max-tries");

            final RandomSearcher randomSearcher = randomTeleport.getRandomSearcher(player, this.world.getSpawnLocation(),
                    minRadius, maxRadius, this.blockValidator); // Don't use our custom validator for now, it seems to cause problems.

            final ValidatorRegistry registry = randomSearcher.getValidators();
            registry.getRaw().clear(); // Clear default validators
            registry.add(this.blockValidator); // Add our own
            randomSearcher.setMaxTries(maxTries);
            //randomSearcher.setMinY(minY); - Don't set this, ignored anyways
            randomSearcher.setMaxY(maxY);

            return randomSearcher.search();
        }

        return CompletableFuture.completedFuture(this.world.getSpawnLocation());
    }


    public static void purgeOldWorlds() {
        try {
            Files.list(Path.of("")).forEach(entry -> {
                // Check if the entry is a file and its name starts with "temp-world-"
                if (Files.isDirectory(entry) && entry.getFileName().toString().startsWith("temp-world-")) {
                    deleteMap(entry.toFile());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CompletableFuture<World> loadMap(String mapName, boolean async) {
        CompletableFuture<World> worldCompletableFuture = new CompletableFuture<>();
        String file = "temp-world-" + UUID.randomUUID();
        Runnable runnable = () -> {
            logger.info("Copying " + mapName + " to " + file);
            try {
                FileUtils.forceDeleteOnExit(new File(file));
                this.copyFolder(Path.of(mapName), Path.of(file));
                logger.info("Finished copying " + mapName + " to " + file);
            } catch (IOException e) {
                logger.log(Level.SEVERE," ERROR COPYING", e);
                worldCompletableFuture.completeExceptionally(e);
            }

            BukkitRunnable bukkitRunnable = new BukkitRunnable(){

                @Override
                public void run() {
                    logger.info("Loading map '" + mapName + "'...");

                    final WorldCreator worldCreator = new WorldCreator(file).keepSpawnLoaded(TriState.FALSE);

                    final World world = Bukkit.createWorld(worldCreator);
                    world.setKeepSpawnInMemory(false);
                    world.setTime(0);
                    world.setStorm(false);
                    world.setThundering(false);
                    world.setAutoSave(false);
                    world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, Boolean.FALSE);
                    world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);

                    worldCompletableFuture.complete(world);
                }
            };

            if (async) {
                bukkitRunnable.runTask(plugin);
            } else {
                bukkitRunnable.run();
            }
        };

        if (async) {
            this.WORLD_LOADER.execute(runnable);
        } else {
            runnable.run();
        }

        return worldCompletableFuture;
    }


    public void copyFolder(Path source, Path target) throws IOException {
        FileUtils.copyDirectory(source.toFile(), target.toFile());
    }

    private static void deleteMap(File file) {
        WORLD_LOADER.execute(() -> {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public World getWorld() {
        return world;
    }

    public String getName() {
        return this.name;
    }

    // These should all be fields, but TODO

    public int getStartTime() {
        return this.mapSetting("start-countdown-seconds");
    }

    public int getPlayersRequiredToStart() {
        return this.mapSetting("players-required-to-start");
    }

    public int maxPlayers() {
        return this.mapSetting("maximum-players-per-round");
    }

    @SuppressWarnings("unchecked")
    public <T> T mapSetting(final String key) {
        final Object value = this.mapSettings.get(key);

        if (value != null) {
            return (T) value;
        }

        return (T) this.fallbackMapSettings.get(key);
    }

    public CompletableFuture<?> reloadMap() {
        World world = this.world;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            chunk.unload(false);
        }
        // validation
        for (Player player : world.getPlayers()) {
            player.kick(Component.text("Invalid state..."));
        }

        CompletableFuture<World> future = new CompletableFuture<>();
        future.thenAccept((velt) -> {
            GameMap.this.world = velt;
        });

        // Unload world
        Bukkit.unloadWorld(world, false);
        this.deleteMap(new File(world.getName()));
        new BukkitRunnable(){

            @Override
            public void run() {
                loadMap(name, true).exceptionally((e) -> {
                    future.completeExceptionally(e);
                    return null;
                }).thenAccept(future::complete);

            }
        }.runTaskLater(plugin, 10);

        return future;
    }
}

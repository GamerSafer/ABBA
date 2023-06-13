package com.gamersafer.minecraft.abbacaving.game.map;

import com.gamersafer.minecraft.abbacaving.game.validators.AbbaValidator;
import de.themoep.randomteleport.RandomTeleport;
import de.themoep.randomteleport.ValidatorRegistry;
import de.themoep.randomteleport.searcher.RandomSearcher;
import net.kyori.adventure.util.TriState;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class GameMap {

    private final String name;
    private final Logger logger;

    private final AbbaValidator blockValidator;
    private final ConfigurationSection mapSettings;
    private final ConfigurationSection fallbackMapSettings;
    private final World world;

    public GameMap(String mapName, ConfigurationSection mapSettings, Logger logger) {
        this.name = mapName;
        this.logger = logger;
        this.fallbackMapSettings = mapSettings.getConfigurationSection("default-settings");
        this.mapSettings = Objects.requireNonNullElse(mapSettings.getConfigurationSection(mapName), this.fallbackMapSettings);

        this.world = this.loadMap(mapName, logger);
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


    private World loadMap(String mapName, Logger logger) {
        logger.info("Loading map '" + mapName + "'...");

        final WorldCreator worldCreator = new WorldCreator(mapName).keepSpawnLoaded(TriState.FALSE);

        final World world = Bukkit.createWorld(worldCreator);
        world.setKeepSpawnInMemory(false);
        world.setTime(0);
        world.setStorm(false);
        world.setThundering(false);
        world.setAutoSave(false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, Boolean.FALSE);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);

        return world;
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

}

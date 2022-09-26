package com.gamersafer.minecraft.abbacaving.game;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.util.Util;
import com.gamersafer.minecraft.abbacaving.worldgen.GiantCavePopulator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class Game {

    private final AbbaCavingPlugin plugin;
    private World world;
    private final String gameId;
    private final Map<String, List<Location>> mapSpawns;
    private final Map<String, GamePlayer> players = new HashMap<>();
    private final boolean generateMap;
    private List<Location> spawns = new ArrayList<>();
    private Map<GamePlayer, Integer> leaderboard = new LinkedHashMap<>();
    private GiantCavePopulator caveGenerator;
    private int counter;
    private boolean gracePeriod;
    private GameState state;

    public Game(final AbbaCavingPlugin plugin, final Map<String, List<Location>> mapSpawns, final String gameId) {
        this.plugin = plugin;
        this.mapSpawns = mapSpawns;
        this.gameId = gameId;

        this.generateMap = plugin.getConfig().getBoolean("cave-generator.enabled");
        if (this.generateMap) {
            this.world = this.generateWorld();
        } else {
            this.world = this.loadRandomMap();
        }

        this.gameState(GameState.RUNNING);
        this.counter(0);

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::nextTick, 0, 20);
    }

    public World world() {
        return this.world;
    }

    public Map<GamePlayer, Integer> leaderboard() {
        return this.leaderboard;
    }

    public boolean isGracePeriod() {
        return this.gracePeriod;
    }

    public GameState gameState() {
        return this.state;
    }

    private void gameState(final GameState state) {
        this.state = state;
    }

    public List<Location> spawnLocations() {
        return this.spawns;
    }

    public Collection<GamePlayer> players() {
        return this.players.values();
    }

    public String gameId() {
        return this.gameId;
    }

    public void addPlayer(final GamePlayer gp) {
        if (this.players.put(gp.player().getName(), gp) == null) {
            this.plugin.broadcast(this.plugin.configMessage("player-joined"), Map.of("player", gp.player().displayName()));

            this.resetPlayer(gp.player());
        }
    }

    public void increasePlayerScore(final GamePlayer player, final int amount) {
        final int currentScore = this.leaderboard.computeIfAbsent(player, x -> 0);

        this.leaderboard.put(player, currentScore + amount);
    }

    public GamePlayer removePlayer(final Player player, final boolean quit) {
        final GamePlayer gp = this.players.get(player.getName());
        if (gp == null) return null;

        this.resetPlayer(player);

        if (!quit) {
            if (this.plugin.hasPermission(player, "respawn") && !gp.hasRespawned()) {
                gp.hasRespawned(true);
                gp.score(0);
                gp.bucketUses(0);
                this.updateLeaderboard();
                this.startingInventory(player);

                final Location loc = this.spawns.get(ThreadLocalRandom.current().nextInt(this.spawns.size()));
                loc.setWorld(this.world);
                gp.spawnLocation(loc);
                player.teleport(loc);

                this.plugin.broadcast(this.plugin.configMessage("player-respawned"), Map.of("player", player.displayName()));
            } else {
                this.players.remove(player.getName());
                this.leaderboard.remove(gp);
                this.updateLeaderboard();

                this.plugin.broadcast(this.plugin.configMessage("player-died"), Map.of("player", player.displayName()));

                if (this.players.size() > 1) {
                    this.plugin.broadcast(this.plugin.configMessage("remaining-players"), Map.of(
                            "count", Component.text(this.players.size()),
                            "optional-s", Component.text(this.players.size() != 1 ? "s" : "")
                    ));
                }
                gp.isDead(true);
                this.sendToLobby(player);
            }
        } else {
            this.players.remove(player.getName());
            this.leaderboard.remove(gp);
            this.updateLeaderboard();

            this.plugin.broadcast(this.plugin.configMessage("player-left"), Map.of("player", player.displayName()));
        }

        return gp;
    }

    public GamePlayer player(final Player player) {
        for (final GamePlayer gp : this.players.values()) {
            if (gp.player().getUniqueId().equals(player.getUniqueId())) {
                return gp;
            }
        }
        return null;
    }

    public void updateScore(final GamePlayer gp) {
        this.leaderboard.put(gp, gp.score());
        this.updateLeaderboard();
    }

    private void counter(final int counter) {
        this.counter = counter;
    }

    public int maxPlayersPerRound() {
        return this.plugin.getConfig().getInt("game.maximum-players-per-round");
    }

    public boolean acceptingNewPlayers() {
        // TODO: config option to disable joining non-full in-progress games
        return this.players.size() < this.maxPlayersPerRound();
    }

    private void nextTick() {
        if (this.state == GameState.RUNNING) {
            final int gameDurationSeconds = this.plugin.getConfig().getInt("game.duration-seconds");
            final int gracePeriodSeconds = this.plugin.getConfig().getInt("game.grace-period-duration-seconds");

            if (this.counter > 0) {
                if (this.gracePeriod) {
                    Bukkit.getServer().sendActionBar(MiniMessage.miniMessage().deserialize("<dark_aqua>Grace Period"));
                } else {
                    Bukkit.getServer().sendActionBar(MiniMessage.miniMessage().deserialize("<gray>Ending In: <green>" + this.counter));
                }
            } else if (this.counter == 0) {
                Bukkit.getServer().sendActionBar(MiniMessage.miniMessage().deserialize("<red>Game Over"));
            }

            if (this.counter == gameDurationSeconds - gracePeriodSeconds) {
                this.gracePeriod = false;
                this.plugin.broadcast(this.plugin.configMessage("grace-period-ended"));
            } else if ((this.counter % 60 == 0 || this.counter == 30 || this.counter == 15
                    || this.counter == 10 || this.counter <= 5) && this.counter != 0) {
                final int minutes = this.counter / 60;
                if (minutes > 0 && minutes <= 5) {
                    this.plugin.broadcast(this.plugin.configMessage("game-ending"), Map.of(
                            "amount", Component.text(minutes),
                            "time-type", Component.text(minutes == 1 ? "minute" : "minutes")
                    ));

                    this.playSound(Sound.UI_BUTTON_CLICK);
                } else if (this.counter <= 30) {
                    this.plugin.broadcast(this.plugin.configMessage("game-ending"), Map.of(
                            "amount", Component.text(this.counter),
                            "time-type", Component.text(this.counter == 1 ? "second" : "seconds")
                    ));

                    this.playSound(Sound.UI_BUTTON_CLICK);
                }
            } else if (this.counter == 0) {
                this.stop();
            }
        }

        this.counter--;
    }

    public void start() {
        this.gracePeriod = true;
        final int gracePeriodSeconds = this.plugin.getConfig().getInt("game.duration-seconds");
        this.counter(gracePeriodSeconds);
        this.plugin.broadcast(this.plugin.configMessage("game-started"));

        final List<Location> spawnsToUse = new ArrayList<>(this.spawns);

        final Iterator<Location> itr = spawnsToUse.iterator();
        while (itr.hasNext()) {
            final Location spawn = itr.next();
            if (spawn.getY() >= 50) {
                this.plugin.getLogger().info(spawn + " spawn is too high");
                itr.remove();
            }
        }
        this.plugin.getLogger().info("Spawns to use: " + spawnsToUse.size());

        for (final GamePlayer gp : this.players.values()) {
            Location loc = null;
            if (!this.generateMap) {
                if (!spawnsToUse.isEmpty()) {
                    loc = spawnsToUse.remove(ThreadLocalRandom.current().nextInt(spawnsToUse.size()));
                } else if (!this.spawns.isEmpty()) {
                    loc = this.spawns.remove(ThreadLocalRandom.current().nextInt(this.spawns.size()));
                } else {
                    loc = this.world.getSpawnLocation();
                }
                loc.setWorld(this.world);
            } else {
                //                loc = caveGenerator.getSpawns().get(ThreadLocalRandom.current().nextInt(caveGenerator.getSpawns().size()));
                //                loc.getChunk().load(true);
                //                spawns.add(loc);
            }

            this.plugin.getLogger().info("Teleporting " + gp.player().getName() + " to " + loc);
            gp.spawnLocation(loc);
            gp.player().teleport(loc);

            this.leaderboard.put(gp, 0);
            this.resetPlayer(gp.player());
            gp.score(0);
            gp.bucketUses(0);
            this.startingInventory(gp.player());
            gp.player().setGameMode(GameMode.SURVIVAL);
        }

        this.updateLeaderboard();

        this.plugin.broadcast(this.plugin.configMessage("game-objective"));
        this.playSound(Sound.BLOCK_NOTE_BLOCK_SNARE);
        this.gameState(GameState.RUNNING);
    }

    private void stop() {
        this.plugin.broadcast(this.plugin.configMessage("game-ended"));

        if (this.leaderboard.size() > 0) {
            final Map<GamePlayer, Integer> sortedLeaderboards =
                    this.leaderboard.entrySet().stream()
                            .sorted(Map.Entry.<GamePlayer, Integer>comparingByValue().reversed())
                            .collect(Collectors.toMap(
                                    Entry::getKey,
                                    Entry::getValue,
                                    (x, y)-> {
                                        throw new AssertionError();
                                    },
                                    LinkedHashMap::new
                            ));

            final Entry<GamePlayer, Integer> winner = sortedLeaderboards.entrySet().iterator().next();
            winner.getKey().wins(winner.getKey().wins() + 1);

            this.plugin.broadcast(this.plugin.configMessage("game-win"), Map.of(
                    "player", winner.getKey().player().displayName(),
                    "score", Component.text(Util.addCommas(winner.getValue())),
                    "optional-s", Component.text(winner.getKey().score() != 1 ? "s" : "")
            ));

            Bukkit.broadcast(Component.empty());

            this.plugin.broadcast("<dark_aqua><bold>TOP PLAYERS</bold>");

            final List<GamePlayer> sorted = new ArrayList<>(sortedLeaderboards.keySet());

            for (int i = 0; i < Math.min(sorted.size(), 5); i++) {
                final GamePlayer gp = sorted.get(i);
                // TODO: replace all broadcasts with world broadcasts
                this.plugin.broadcast("<gray>#" + (i + 1) + ": <green><displayname> <gray>- " + Util.addCommas(this.leaderboard.get(gp)),
                        TagResolver.resolver("displayname", Tag.inserting(gp.player().displayName())));
            }
        } else {
            this.plugin.broadcast("<green>The game has ended in a draw!");
        }

        for (final GamePlayer gp : this.players.values()) {
            this.resetPlayer(gp.player());
        }

        this.gameState(GameState.DONE);
        this.counter(0);

        this.plugin.broadcast("<green>Returning to lobby in 10 seconds...");

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            for (final GamePlayer gp : this.players.values()) {
                this.sendToLobby(gp.player());
            }

            this.plugin.lobby().stop(this);
            Util.deleteWorld(this.world);
        }, 20 * 10);
    }

    private World generateWorld() {
        this.plugin.getLogger().info("Deleting existing world...");

        if (this.world != null) {
            Util.deleteWorld(this.world);
        }

        this.plugin.getLogger().info("Done");

        this.plugin.getLogger().info("Creating new world...");
        this.world = this.plugin.getServer().createWorld(new WorldCreator(this.gameId));

        this.plugin.getLogger().info("Attaching cave populator to world \"" + this.world.getName() + "\"");
        this.caveGenerator = new GiantCavePopulator(this.plugin, this.world);
        this.world.getPopulators().add(this.caveGenerator);
        this.plugin.getLogger().info("Done");

        return this.world;
    }

    private World loadRandomMap() {
        final File mapsDir = new File(this.plugin.getDataFolder(), "maps");

        if (!mapsDir.exists()) {
            mapsDir.mkdirs();
        }

        final File[] mapArchives = mapsDir.listFiles();
        final File mapArchive = mapArchives[ThreadLocalRandom.current().nextInt(mapArchives.length)];

        // TODO: make sure a map loads
        if (!mapArchive.exists()) {
            this.plugin.getLogger().warning("Map archive not found: " + mapArchive.getPath());
            return null;
        }

        this.plugin.getLogger().info("Loading map '" + mapArchive.getName() + "'...");
        final String mapName = mapArchive.getName().substring(0, mapArchive.getName().lastIndexOf('.'));

        this.spawns = Objects.requireNonNullElseGet(this.mapSpawns.get(mapName), List::of);
        this.plugin.getLogger().info("Loaded " + this.spawns.size() + " spawns(s) for map " + mapName);

        final World world = Util.loadMap(mapArchive, this.gameId);

        int removed = 0;
        int items = 0;

        for (final Entity e : world.getEntities()) {
            if (e.getType() == EntityType.DROPPED_ITEM) {
                items++;
            }
            removed++;
            e.remove();
        }

        this.plugin.getLogger().info("Removed " + removed + " entities (" + items + " were items)");
        this.plugin.getLogger().info("Created map from " + mapArchive);

        return world;
    }

    private void playSound(final Sound sound) {
        for (final GamePlayer gp : this.players.values()) {
            gp.player().playSound(gp.player().getLocation(), sound, 1f, 1f);
        }
    }

    private void startingInventory(final Player player) {
        final Inventory inv = player.getInventory();

        final ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        final ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        pickaxeMeta.setUnbreakable(true);
        pickaxe.setItemMeta(pickaxeMeta);
        pickaxe.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);

        final ItemStack bow = new ItemStack(Material.BOW);
        final ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.setUnbreakable(true);
        bow.setItemMeta(bowMeta);
        bow.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);

        final ItemStack shield = new ItemStack(Material.SHIELD);
        final Damageable damageable = (Damageable) shield.getItemMeta();
        damageable.setDamage(168);
        shield.setItemMeta(damageable);
        player.getInventory().setItemInOffHand(shield);

        inv.addItem(Util.displayName(pickaxe, "<green><bold>Starter Pickaxe"));
        inv.addItem(Util.displayName(new ItemStack(Material.IRON_SWORD), "<green><bold>Starter Sword"));
        inv.addItem(Util.displayName(bow, "<green><bold>Infinite Bow"));
        inv.addItem(Util.displayName(new ItemStack(Material.IRON_SHOVEL), "<green><bold>Starter Shovel"));
        inv.addItem(Util.displayName(new ItemStack(Material.COOKED_BEEF), "<green><bold>Infinite Steak Supply"));
        inv.addItem(Util.displayName(new ItemStack(Material.COBBLESTONE), "<green><bold>Infinite Cobble"));
        inv.addItem(new ItemStack(Material.WATER_BUCKET));
        inv.addItem(Util.displayName(new ItemStack(Material.TORCH), "<green><bold>Infinite Torch"));
        inv.setItem(35, new ItemStack(Material.ARROW, 1));

        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    public void resetPlayer(final Player player) {
        final int maxHealth = this.plugin.getConfig().getInt("game.player-health");
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setLevel(0);
        player.setExp(0);
        player.getInventory().clear();
    }

    private void updateLeaderboard() {
        this.leaderboard = Util.sortByValue(this.leaderboard, true);
    }

    private void sendToLobby(final Player player) {
        player.teleport(new Location(
                Bukkit.getWorld(this.plugin.getConfig().getString("lobby-spawn-location.world")),
                this.plugin.getConfig().getDouble("lobby-spawn-location.x"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.y"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.z"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.yaw"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.pitch")));
    }

}

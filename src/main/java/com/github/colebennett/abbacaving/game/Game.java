package com.github.colebennett.abbacaving.game;

import be.maximvdw.featherboard.api.FeatherBoardAPI;
import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.util.Util;
import com.github.colebennett.abbacaving.worldgen.GiantCavePopulator;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Game {

    private final AbbaCavingPlugin plugin;
    private final Map<String, List<Location>> mapSpawns;
    private final Map<String, GamePlayer> players = new HashMap<>();
    private final boolean generateMap;
    private List<Location> spawns = new ArrayList<>();
    private Map<GamePlayer, Integer> leaderboard = new LinkedHashMap<>();
    private GiantCavePopulator caveGenerator;
    private int counter;
    private boolean gracePeriod;
    private GameState state;

    public Game(final AbbaCavingPlugin plugin, final Map<String, List<Location>> mapSpawns) {
        this.plugin = plugin;
        this.mapSpawns = mapSpawns;

        this.generateMap = plugin.getConfig().getBoolean("cave-generator.enabled");
        if (this.generateMap) {
            this.generateWorld();
        } else {
            this.loadRandomMap();
        }

        this.gameState(GameState.WAITING);
        this.counter(0);
        plugin.updateGameInfo("slots", Integer.toString(plugin.getServer().getMaxPlayers()));

        final String key = AbbaCavingPlugin.REDIS_TAG + ":servers";
        plugin.jedis().sadd(key, plugin.serverId());
        final String serverList = String.join(",", plugin.jedis().smembers(key));
        plugin.jedis().publish(AbbaCavingPlugin.REDIS_TAG, String.format("%s,%s", key, serverList));
        plugin.getLogger().info("[Redis] Updated server list: " + serverList);

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::nextTick, 0, 20);
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
        this.plugin.updateGameInfo("state", state.name());

        for (final GamePlayer gp : this.players.values()) {
            this.updateScoreboard(gp.player());
        }
    }

    public List<Location> spawnLocations() {
        return this.spawns;
    }

    public Collection<GamePlayer> players() {
        return this.players.values();
    }

    public void addPlayer(final GamePlayer gp) {
        if (this.players.put(gp.player().getName(), gp) == null) {
            this.plugin.broadcast(this.plugin.configMessage("player-joined"), Map.of("player", gp.player().displayName()));

            this.resetPlayer(gp.player());
            this.updateScoreboard(gp.player());

            this.plugin.updateGameInfo("players", Integer.toString(this.players.size()));
        }
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
                loc.setWorld(this.plugin.getServer().getWorld(this.plugin.gameWorldName()));
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

        this.plugin.updateGameInfo("players", Integer.toString(this.players.size()));
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
        this.plugin.updateGameInfo("counter", Integer.toString(counter));
    }

    private void nextTick() {
        if (this.state == GameState.WAITING) {
            if (this.players.values().size() >= this.plugin.getConfig().getInt("game.players-required-to-start")) {
                //                if (caveGenerator != null && !caveGenerator.isReady()) {
                //                    plugin.getLogger().info("Waiting for the world to be generated...");
                //                } else {
                //                    preStart();
                //                }
                this.preStart();
            }
        } else if (this.state == GameState.STARTING) {
            if (this.counter >= 0) {
                Bukkit.getServer().sendActionBar(MiniMessage.miniMessage().deserialize("<gray>Starting In: <green>" + this.counter));
            }

            if (this.counter == 0) {
                this.start();
            } else {
                if (this.counter % 60 == 0 || this.counter == 30 || this.counter == 15
                        || this.counter == 10 || this.counter <= 5) {
                    this.plugin.broadcast(this.plugin.configMessage("game-starting"), Map.of(
                            "seconds", Component.text(this.counter),
                            "optional-s", Component.text(this.counter != 1 ? "s" : "")
                    ));
                }
            }
        } else if (this.state == GameState.RUNNING) {
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

        if (this.state == GameState.STARTING || this.state == GameState.RUNNING) {
            if (this.counter <= 10 || this.counter % 5 == 0) {
                this.plugin.updateGameInfo("counter", Integer.toString(this.counter));
            }
        }
        this.counter--;
    }

    public void preStart() {
        this.counter(this.plugin.getConfig().getInt("game.start-countdown-seconds"));
        this.gameState(GameState.STARTING);
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
                } else {
                    loc = this.spawns.remove(ThreadLocalRandom.current().nextInt(this.spawns.size()));
                }
                loc.setWorld(this.plugin.getServer().getWorld(this.plugin.gameWorldName()));
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

        //        String serverName = RedisApi.instance().getServerName();
        //        String newName = serverName.replace("pregame", "ingame");
        //        RedisApi.instance().unregisterServer(serverName);
        //        RedisApi.instance().renameServer(newName);
        String ip = this.plugin.getServer().getIp();
        if (ip.isEmpty()) {
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                ip = socket.getLocalAddress().getHostAddress();
            } catch (final UnknownHostException | SocketException e) {
                e.printStackTrace();
            }
        }

        if (ip.isEmpty()) {
            ip = "localhost";
        }
        //RedisApi.instance().registerServer(newName, ip, Integer.toString(plugin.getServer().getPort()));
    }

    private void stop() {
        this.plugin.broadcast(this.plugin.configMessage("game-ended"));

        if (this.leaderboard.size() > 0) {
            final Entry<GamePlayer, Integer> winner = this.leaderboard.entrySet().iterator().next();
            winner.getKey().wins(winner.getKey().wins() + 1);
            this.plugin.broadcast(this.plugin.configMessage("game-win"), Map.of(
                    "player", winner.getKey().player().displayName(),
                    "score", Component.text(Util.addCommas(winner.getValue())),
                    "optional-s", Component.text(winner.getKey().score() != 1 ? "s" : "")
            ));

            Bukkit.broadcast(Component.empty());

            this.plugin.broadcast("<dark_aqua><bold>TOP PLAYERS</bold>");

            final List<GamePlayer> sorted = new ArrayList<>(this.leaderboard.keySet());

            for (int i = 0; i < Math.min(sorted.size(), 5); i++) {
                final GamePlayer gp = sorted.get(i);
                this.plugin.broadcast("<gray>#" + (i + 1) + ": <green>" + gp.player().getDisplayName() + " <gray>- " + Util.addCommas(this.leaderboard.get(gp)));
            }
        } else {
            this.plugin.broadcast("<green>The game has ended in a draw!");
        }

        for (final GamePlayer gp : this.players.values()) {
            this.resetPlayer(gp.player());
        }

        this.gameState(GameState.DONE);
        this.counter(0);

        this.plugin.broadcast("<green>Server restarting in 10 seconds...");

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            for (final GamePlayer gp : this.players.values()) {
                this.sendToLobby(gp.player());
            }

            this.plugin.getLogger().info("Shutting the server down in 5 seconds...");
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                this.plugin.getLogger().info("Shutting the the server down");
                this.plugin.getServer().shutdown();
            }, 20 * 5);
        }, 20 * 10);
    }

    private void generateWorld() {
        this.plugin.getLogger().info("Deleting existing world...");
        final World currentWorld = Bukkit.getWorld(this.plugin.gameWorldName());
        if (currentWorld != null) {
            Util.deleteWorld(currentWorld);
        } else {
            Util.deleteWorld(new File(this.plugin.gameWorldName()));
        }
        this.plugin.getLogger().info("Done");

        this.plugin.getLogger().info("Creating new world...");
        final World world = this.plugin.getServer().createWorld(new WorldCreator(this.plugin.gameWorldName()));

        this.plugin.getLogger().info("Attaching cave populator to world \"" + world.getName() + "\"");
        this.caveGenerator = new GiantCavePopulator(this.plugin, world);
        world.getPopulators().add(this.caveGenerator);
        this.plugin.getLogger().info("Done");

        //        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldBorder")) {
        //            int ticks = 1;
        //            int defaultPadding = CoordXZ.chunkToBlock(13);
        //            WorldFillTask fillTask = new WorldFillTask(Bukkit.getServer(), null, world.getName(), defaultPadding, 1, ticks, true);
        //            int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, fillTask, ticks, ticks);
        //            plugin.getLogger().info("Created world fill task: id=" + taskId);
        //        }
    }

    private void loadRandomMap() {
        final String mapsDirAbsPath = this.plugin.getConfig().getString("maps-directory", "");
        final File mapsDir;
        if (!mapsDirAbsPath.isEmpty()) {
            mapsDir = new File(mapsDirAbsPath);
        } else {
            mapsDir = new File(this.plugin.getDataFolder(), "maps");
        }

        final File[] mapArchives = mapsDir.listFiles();
        final File mapArchive = mapArchives[ThreadLocalRandom.current().nextInt(mapArchives.length)];
        if (!mapArchive.exists()) {
            this.plugin.getLogger().warning("Map archive not found: " + mapArchive.getPath());
            return;
        }

        this.plugin.getLogger().info("Loading map '" + mapArchive.getName() + "'...");
        final String mapName = mapArchive.getName().substring(0, mapArchive.getName().lastIndexOf('.'));
        this.spawns = this.mapSpawns.get(mapName);
        this.plugin.getLogger().info("Loaded " + this.spawns.size() + " spawns(s) for map " + mapName);

        final World world = Util.loadMap(mapArchive, this.plugin.gameWorldName());

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
        shield.setDurability((short) 168);
        player.getInventory().setItemInOffHand(shield);

        inv.addItem(Util.displayName(pickaxe, "&a&lStarter Pickaxe"));
        inv.addItem(Util.displayName(new ItemStack(Material.IRON_SWORD), "&a&lStarter Sword"));
        inv.addItem(Util.displayName(bow, "&a&lInfinite Bow"));
        inv.addItem(Util.displayName(new ItemStack(Material.IRON_SHOVEL), "&a&lStarter Shovel"));
        inv.addItem(Util.displayName(new ItemStack(Material.COOKED_BEEF), "&a&lInfinite Steak Supply"));
        inv.addItem(new ItemStack(Material.COBBLESTONE, 32));
        inv.addItem(new ItemStack(Material.WATER_BUCKET));
        inv.addItem(Util.displayName(new ItemStack(Material.CRAFTING_TABLE), "&a&lInfinite Crafting Table"));
        inv.addItem(Util.displayName(new ItemStack(Material.TORCH), "&a&lInfinite Torch"));
        inv.setItem(35, new ItemStack(Material.ARROW, 1));

        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    public void resetPlayer(final Player player) {
        final int maxHealth = this.plugin.getConfig().getInt("game.player-health");
        player.setMaxHealth(maxHealth);
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

    private void showFeatherBoard(final Player player, final String name) {
        if (this.plugin.getServer().getPluginManager().getPlugin("FeatherBoard") != null) {
            FeatherBoardAPI.showScoreboard(player, name);
        } else {
            this.plugin.getLogger().warning("FeatherBoard plugin not found");
        }
    }

    private void updateScoreboard(final Player player) {
        if (this.plugin.luckPermsAPI() != null) {
            final ConfigurationSection configSection = this.plugin.getConfig().getConfigurationSection("scoreboards");
            final String stateName = this.state.name().toLowerCase();
            final String defaultScoreboardName = configSection.getString(stateName + ".default.scoreboard-name");
            final String donorScoreboardName = configSection.getString(stateName + ".donor.scoreboard-name", "");
            final String donorScoreboardPerm = configSection.getString(stateName + ".donor.permission", "");

            String scoreboardName = defaultScoreboardName;

            if (!donorScoreboardPerm.isEmpty()) {
                if (this.plugin.hasPermission(player, donorScoreboardPerm)) {
                    scoreboardName = donorScoreboardName;
                }
            }

            this.showFeatherBoard(player, scoreboardName);
        }
    }

    private void sendToLobby(final Player player) {
        final List<String> lobbyServers = this.plugin.getConfig().getStringList("lobby-servers");
        final String selectedServer = lobbyServers.get(ThreadLocalRandom.current().nextInt(lobbyServers.size()));

        this.plugin.message(player, "<green>Sending you to " + selectedServer);
        this.plugin.getLogger().info(String.format("Sending %s to %s...", player.getName(), selectedServer));

        try (
                final ByteArrayOutputStream b = new ByteArrayOutputStream();
                final DataOutputStream out = new DataOutputStream(b)
        ) {
            out.writeUTF("Connect");
            out.writeUTF(selectedServer);
            player.sendPluginMessage(this.plugin, "BungeeCord", b.toByteArray());

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}

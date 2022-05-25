package com.github.colebennett.abbacaving.game;

import be.maximvdw.featherboard.api.FeatherBoardAPI;
import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.util.Util;
import com.github.colebennett.abbacaving.worldgen.GiantCavePopulator;
import dev.netherite.redis.spigot.RedisApi;
import io.netty.util.internal.ThreadLocalRandom;
import joptsimple.internal.Strings;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;

public class Game {

    private final AbbaCavingPlugin plugin;
    private final Map<String, List<Location>> mapSpawns;
    private final Map<String, GamePlayer> players = new HashMap<>();
    private List<Location> spawns = new ArrayList<>();
    private Map<GamePlayer, Integer> leaderboard = new LinkedHashMap<>();

    private GiantCavePopulator caveGenerator;
    private int counter;
    private boolean gracePeriod;
    private GameState state;
    private final boolean generateMap;

    public Game(AbbaCavingPlugin plugin, Map<String, List<Location>> mapSpawns) {
        this.plugin = plugin;
        this.mapSpawns = mapSpawns;

        generateMap = plugin.getConfig().getBoolean("cave-generator.enabled");
        if (generateMap) {
            generateWorld();
        } else {
            loadRandomMap();
        }

        setState(GameState.WAITING);
        setCounter(0);
        plugin.updateGameInfo("slots", Integer.toString(plugin.getServer().getMaxPlayers()));

        String key = AbbaCavingPlugin.REDIS_TAG + ":servers";
        plugin.getJedis().sadd(key, plugin.getServerId());
        String serverList = Strings.join(plugin.getJedis().smembers(key), ",");
        plugin.getJedis().publish(AbbaCavingPlugin.REDIS_TAG, String.format("%s,%s", key, serverList));
        plugin.getLogger().info("[Redis] Updated server list: " + serverList);

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::nextTick, 0, 20);
    }

    public Map<GamePlayer, Integer> getLeaderboard() {
        return leaderboard;
    }

    public boolean isGracePeriod() {
       return gracePeriod;
    }

    public GameState getState() {
       return state;
    }

    public List<Location> getSpawns() {
        return spawns;
    }

    public Collection<GamePlayer> getPlayers() {
        return players.values();
    }

    public void addPlayer(GamePlayer gp) {
        if (players.put(gp.getPlayer().getName(), gp) == null) {
            plugin.broadcast(plugin.getMessage("player-joined"), new HashMap<String, String>() {{
                put("player", gp.getPlayer().getDisplayName());
            }});

            resetPlayer(gp.getPlayer());
            updateScoreboard(gp.getPlayer());

            plugin.updateGameInfo("players", Integer.toString(players.size()));
        }
    }

    public GamePlayer removePlayer(Player player, boolean quit) {
        GamePlayer gp = players.get(player.getName());
        if (gp == null) return null;

        resetPlayer(player);

        if (!quit) {
            if (plugin.hasPermission(player, "respawn") && !gp.hasRespawned()) {
                gp.setHasRespawned(true);
                gp.setScore(0);
                gp.setBucketUses(0);
                updateLeaderboard();
                setStartingInventory(player);

                Location loc = spawns.get(ThreadLocalRandom.current().nextInt(spawns.size()));
                loc.setWorld(plugin.getServer().getWorld(plugin.getGameWorldName()));
                gp.setSpawn(loc);
                player.teleport(loc);

                plugin.broadcast(plugin.getMessage("player-respawned"), new HashMap<String, String>() {{
                    put("player", player.getDisplayName());
                }});
            } else {
                players.remove(player.getName());
                leaderboard.remove(gp);
                updateLeaderboard();

                plugin.broadcast(plugin.getMessage("player-died"), new HashMap<String, String>() {{
                    put("player", player.getDisplayName());
                }});
                if (players.size() > 1) {
                    plugin.broadcast(plugin.getMessage("remaining-players"), new HashMap<String, String>() {{
                        put("count", Integer.toString(players.size()));
                        put("optional-s", players.size() != 1 ? "s" : "");
                    }});
                }
                gp.setIsDead(true);
                sendToLobby(player);
            }
        } else {
            players.remove(player.getName());
            leaderboard.remove(gp);
            updateLeaderboard();

            plugin.broadcast(plugin.getMessage("player-left"), new HashMap<String, String>() {{
                put("player", player.getDisplayName());
            }});
        }

        plugin.updateGameInfo("players", Integer.toString(players.size()));
        return gp;
    }

    public GamePlayer getPlayer(Player player) {
       for (GamePlayer gp : players.values()) {
           if (gp.getPlayer().getUniqueId().equals(player.getUniqueId())) {
               return gp;
           }
       }
       return null;
    }

    public void updateScore(GamePlayer gp) {
        leaderboard.put(gp, gp.getScore());
        updateLeaderboard();
    }

    private void setCounter(int counter) {
        this.counter = counter;
        plugin.updateGameInfo("counter", Integer.toString(counter));
    }

    private void setState(GameState state) {
        this.state = state;
        plugin.updateGameInfo("state", state.name());

        for (GamePlayer gp : players.values()) {
            updateScoreboard(gp.getPlayer());
        }
    }

    private void nextTick() {
        if (state == GameState.WAITING) {
            if (players.values().size() >= plugin.getConfig().getInt("game.players-required-to-start")) {
//                if (caveGenerator != null && !caveGenerator.isReady()) {
//                    plugin.getLogger().info("Waiting for the world to be generated...");
//                } else {
//                    preStart();
//                }
                preStart();
            }
        } else if (state == GameState.STARTING) {
            if (counter >= 0) {
                BukkitAudiences audiences = BukkitAudiences.create(plugin);
                audiences.all().sendActionBar(MiniMessage.get().parse("<gray>Starting In: <green>" + counter));
            }

            if (counter == 0) {
                start();
            } else {
                if ((counter % 60 == 0 || counter == 30 || counter == 15
                        || counter == 10 || counter <= 5)) {
                    plugin.broadcast(plugin.getMessage("game-starting"), new HashMap<String, String>() {{
                        put("seconds", Integer.toString(counter));
                        put("optional-s", counter != 1 ? "s" : "");
                    }});
                }
            }
        } else if (state == GameState.RUNNING) {
            int gameDurationSeconds = plugin.getConfig().getInt("game.duration-seconds");
            int gracePeriodSeconds = plugin.getConfig().getInt("game.grace-period-duration-seconds");

            BukkitAudiences audiences = BukkitAudiences.create(plugin);
            if (counter > 0) {
                if (gracePeriod) {
                    audiences.all().sendActionBar(MiniMessage.get().parse("<dark_aqua>Grace Period"));
                } else {
                    audiences.all().sendActionBar(MiniMessage.get().parse("<gray>Ending In: <green>" + counter));
                }
            } else if (counter == 0) {
                audiences.all().sendActionBar(MiniMessage.get().parse("<red>Game Over"));
            }

            if (counter == gameDurationSeconds - gracePeriodSeconds) {
                gracePeriod = false;
                plugin.broadcast(plugin.getMessage("grace-period-ended"));
            } else if ((counter % 60 == 0 || counter == 30 || counter == 15
                    || counter == 10 || counter <= 5) && counter != 0) {
                int minutes = counter / 60;
                if (minutes > 0 && minutes <= 5) {
                    plugin.broadcast(plugin.getMessage("game-ending"), new HashMap<String, String>() {{
                        put("amount", Integer.toString(minutes));
                        put("time-type", minutes == 1 ? "minute" : "minutes");
                    }});
                    playSound(Sound.UI_BUTTON_CLICK);
                } else if (counter <= 30) {
                    plugin.broadcast(plugin.getMessage("game-ending"), new HashMap<String, String>() {{
                        put("amount", Integer.toString(counter));
                        put("time-type", counter == 1 ? "second" : "seconds");
                    }});
                    playSound(Sound.UI_BUTTON_CLICK);
                }
            } else if (counter == 0) {
                stop();
            }
        }

        if (state == GameState.STARTING || state == GameState.RUNNING) {
            if (counter <= 10 || counter % 5 == 0) {
                plugin.updateGameInfo("counter", Integer.toString(counter));
            }
        }
        counter--;
    }

    public void preStart() {
        setCounter(plugin.getConfig().getInt("game.start-countdown-seconds"));
        setState(GameState.STARTING);
    }

    public void start() {
        gracePeriod = true;
        int gracePeriodSeconds = plugin.getConfig().getInt("game.duration-seconds");
        setCounter(gracePeriodSeconds);
        plugin.broadcast(plugin.getMessage("game-started"));

        List<Location> spawnsToUse = new ArrayList<>(spawns);

        Iterator<Location> itr = spawnsToUse.iterator();
        while (itr.hasNext()) {
            Location spawn = itr.next();
            if (spawn.getY() >= 50) {
                plugin.getLogger().info(spawn + " spawn is too high");
                itr.remove();
            }
        }
        plugin.getLogger().info("Spawns to use: " + spawnsToUse.size());

        for (GamePlayer gp : players.values()) {
            Location loc = null;
            if (!generateMap) {
                if (!spawnsToUse.isEmpty()) {
                    loc = spawnsToUse.remove(ThreadLocalRandom.current().nextInt(spawnsToUse.size()));
                } else {
                    loc = spawns.remove(ThreadLocalRandom.current().nextInt(spawns.size()));
                }
                loc.setWorld(plugin.getServer().getWorld(plugin.getGameWorldName()));
            } else {
//                loc = caveGenerator.getSpawns().get(ThreadLocalRandom.current().nextInt(caveGenerator.getSpawns().size()));
//                loc.getChunk().load(true);
//                spawns.add(loc);
            }

            plugin.getLogger().info("Teleporting " + gp.getPlayer().getName() + " to " + loc);
            gp.setSpawn(loc);
            gp.getPlayer().teleport(loc);

            leaderboard.put(gp, 0);
            resetPlayer(gp.getPlayer());
            gp.setScore(0);
            gp.setBucketUses(0);
            setStartingInventory(gp.getPlayer());
            gp.getPlayer().setGameMode(GameMode.SURVIVAL);
        }

        updateLeaderboard();

        plugin.broadcast(plugin.getMessage("game-objective"));
        playSound(Sound.BLOCK_NOTE_BLOCK_SNARE);
        setState(GameState.RUNNING);

        String serverName = RedisApi.instance().getServerName();
        String newName = serverName.replace("pregame", "ingame");
        RedisApi.instance().unregisterServer(serverName);
        RedisApi.instance().renameServer(newName);
        String ip = plugin.getServer().getIp();
        if (ip.isEmpty()) {
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                ip = socket.getLocalAddress().getHostAddress();
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            }
        }

        if (ip.isEmpty()) {
            ip = "localhost";
        }
        RedisApi.instance().registerServer(newName, ip, Integer.toString(plugin.getServer().getPort()));
    }

    private void stop() {
        plugin.broadcast(plugin.getMessage("game-ended"));

        if (leaderboard.size() > 0) {
            Entry<GamePlayer, Integer> winner = leaderboard.entrySet().iterator().next();
            winner.getKey().setWins(winner.getKey().getWins() + 1);
            plugin.broadcast(plugin.getMessage("game-win"), new HashMap<String, String>() {{
                put("player", winner.getKey().getPlayer().getDisplayName());
                put("score", Util.addCommas(winner.getValue()));
                put("optional-s", winner.getKey().getScore() != 1 ? "s" : "");
            }});

            Bukkit.broadcastMessage("");
            plugin.broadcast("<dark_aqua><bold>TOP PLAYERS</bold>");
            List<GamePlayer> sorted = new ArrayList<>(leaderboard.keySet());
            for (int i = 0; i < Math.min(sorted.size(), 5); i++) {
                GamePlayer gp = sorted.get(i);
                plugin.broadcast("<gray>#" + (i + 1) + ": <green>" + gp.getPlayer().getDisplayName() + " <gray>- " + Util.addCommas(leaderboard.get(gp)));
            }
        } else {
            plugin.broadcast("<green>The game has ended in a draw!");
        }

        for (GamePlayer gp : players.values()) {
            resetPlayer(gp.getPlayer());
        }

        setState(GameState.DONE);
        setCounter(0);

        plugin.broadcast("<green>Server restarting in 10 seconds...");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (GamePlayer gp : players.values()) {
                sendToLobby(gp.getPlayer());
            }

            plugin.getLogger().info("Shutting the server down in 5 seconds...");
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getLogger().info("Shutting the the server down");
                plugin.getServer().shutdown();
            }, 20 * 5);
        }, 20 * 10);
    }

    private void generateWorld() {
        plugin.getLogger().info("Deleting existing world...");
        World currentWorld = Bukkit.getWorld(plugin.getGameWorldName());
        if (currentWorld != null) {
            Util.deleteWorld(currentWorld);
        } else {
            Util.deleteWorld(new File(plugin.getGameWorldName()));
        }
        plugin.getLogger().info("Done");

        plugin.getLogger().info("Creating new world...");
        World world = plugin.getServer().createWorld(new WorldCreator(plugin.getGameWorldName()));

        plugin.getLogger().info("Attaching cave populator to world \"" + world.getName() + "\"");
        caveGenerator = new GiantCavePopulator(plugin, world);
        world.getPopulators().add(caveGenerator);
        plugin.getLogger().info("Done");

//        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldBorder")) {
//            int ticks = 1;
//            int defaultPadding = CoordXZ.chunkToBlock(13);
//            WorldFillTask fillTask = new WorldFillTask(Bukkit.getServer(), null, world.getName(), defaultPadding, 1, ticks, true);
//            int taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, fillTask, ticks, ticks);
//            plugin.getLogger().info("Created world fill task: id=" + taskId);
//        }
    }

    private void loadRandomMap() {
        String mapsDirAbsPath = plugin.getConfig().getString("maps-directory", "");
        File mapsDir;
        if (!mapsDirAbsPath.isEmpty()) {
            mapsDir = new File(mapsDirAbsPath);
        } else {
            mapsDir = new File(plugin.getDataFolder(), "maps");
        }

        File[] mapArchives = mapsDir.listFiles();
        File mapArchive = mapArchives[ThreadLocalRandom.current().nextInt(mapArchives.length)];
        if (!mapArchive.exists()) {
            plugin.getLogger().warning("Map archive not found: " + mapArchive.getPath());
            return;
        }

        plugin.getLogger().info("Loading map '" + mapArchive.getName() + "'...");
        String mapName = mapArchive.getName().substring(0, mapArchive.getName().lastIndexOf('.'));
        spawns = mapSpawns.get(mapName);
        plugin.getLogger().info("Loaded " + spawns.size() + " spawns(s) for map " + mapName);

        World world = Util.loadMap(mapArchive, plugin.getGameWorldName());

        int removed = 0, items = 0;
        for (Entity e : world.getEntities()) {
            if (e.getType() == EntityType.DROPPED_ITEM) {
                items++;
            }
            removed++;
            e.remove();
        }
        plugin.getLogger().info("Removed " + removed + " entities (" + items + " were items)");
        plugin.getLogger().info("Created map from " + mapArchive);
    }

    private void playSound(Sound sound) {
        for (GamePlayer gp : players.values()) {
            gp.getPlayer().playSound(gp.getPlayer().getLocation(), sound, 1f, 1f);
        }
    }

    private void setStartingInventory(Player player) {
        Inventory inv = player.getInventory();

        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        pickaxeMeta.setUnbreakable(true);
        pickaxe.setItemMeta(pickaxeMeta);
        pickaxe.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.setUnbreakable(true);
        bow.setItemMeta(bowMeta);
        bow.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);

        ItemStack shield = new ItemStack(Material.SHIELD);
        shield.setDurability((short) 168);
        player.getInventory().setItemInOffHand(shield);

        inv.addItem(Util.setDisplayName(pickaxe, "&a&lStarter Pickaxe"));
        inv.addItem(Util.setDisplayName(new ItemStack(Material.IRON_SWORD), "&a&lStarter Sword"));
        inv.addItem(Util.setDisplayName(bow, "&a&lInfinite Bow"));
        inv.addItem(Util.setDisplayName(new ItemStack(Material.IRON_SHOVEL), "&a&lStarter Shovel"));
        inv.addItem(Util.setDisplayName(new ItemStack(Material.COOKED_BEEF), "&a&lInfinite Steak Supply"));
        inv.addItem(new ItemStack(Material.COBBLESTONE, 32));
        inv.addItem(new ItemStack(Material.WATER_BUCKET));
        inv.addItem(Util.setDisplayName(new ItemStack(Material.CRAFTING_TABLE), "&a&lInfinite Crafting Table"));
        inv.addItem(Util.setDisplayName(new ItemStack(Material.TORCH), "&a&lInfinite Torch"));
        inv.setItem(35, new ItemStack(Material.ARROW, 1));

        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    public void resetPlayer(Player player) {
        int maxHealth = plugin.getConfig().getInt("game.player-health");
        player.setMaxHealth(maxHealth);
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setLevel(0);
        player.setExp(0);
        player.getInventory().clear();
    }

    private void updateLeaderboard() {
        leaderboard = Util.sortByValue(leaderboard, true);
    }

    private void showFeatherBoard(Player player, String name) {
        if (plugin.getServer().getPluginManager().getPlugin("FeatherBoard") != null) {
            FeatherBoardAPI.showScoreboard(player, name);
        } else {
            plugin.getLogger().warning("FeatherBoard plugin not found");
        }
    }

    private void updateScoreboard(Player player) {
        if (plugin.getLuckPermsApi() != null) {
            ConfigurationSection configSection = plugin.getConfig().getConfigurationSection("scoreboards");
            String stateName = state.name().toLowerCase();
            String defaultScoreboardName = configSection.getString(stateName + ".default.scoreboard-name");
            String donorScoreboardName = configSection.getString(stateName + ".donor.scoreboard-name", "");
            String donorScoreboardPerm = configSection.getString(stateName + ".donor.permission", "");

            String scoreboardName = defaultScoreboardName;

            if (!donorScoreboardPerm.isEmpty()) {
                if (plugin.hasPermission(player, donorScoreboardPerm)) {
                    scoreboardName = donorScoreboardName;
                }
            }

            showFeatherBoard(player, scoreboardName);
        }
    }

    private void sendToLobby(Player player) {
        List<String> lobbyServers = plugin.getConfig().getStringList("lobby-servers");
        String selectedServer = lobbyServers.get(ThreadLocalRandom.current().nextInt(lobbyServers.size()));

        plugin.message(player, "<green>Sending you to " + selectedServer);
        plugin.getLogger().info(String.format("Sending %s to %s...", player.getName(), selectedServer));

        try (
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b)
        ) {
            out.writeUTF("Connect");
            out.writeUTF(selectedServer);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

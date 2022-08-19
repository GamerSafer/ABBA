package com.github.colebennett.abbacaving;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import com.github.colebennett.abbacaving.commands.ForceStartCommand;
import com.github.colebennett.abbacaving.commands.NightVisionCommand;
import com.github.colebennett.abbacaving.commands.PointsCommand;
import com.github.colebennett.abbacaving.commands.StatsCommand;
import com.github.colebennett.abbacaving.game.*;
import com.github.colebennett.abbacaving.listeners.*;
import com.github.colebennett.abbacaving.util.Util;
import com.github.colebennett.abbacaving.worldgen.GiantCavePopulator;
import com.github.colebennett.abbacaving.placeholders.GamePlaceholders;
import com.github.colebennett.abbacaving.placeholders.LobbyPlaceholders;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AbbaCavingPlugin extends JavaPlugin {

    private enum ServerMode {
        GAME, LOBBY
    }

    public static final String REDIS_TAG = "abbacaving";

    private Set<CaveOre> ores;
    private Set<CaveLoot> loot;
    private Map<String, List<Location>> mapSpawns;
    private Game game;
    private Jedis jedis;
    private LuckPerms luckPermsApi;
    private HikariDataSource dataSource;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getConfig().getBoolean("cave-generator.generator-mode")) {
            getLogger().info("Generator mode");

            for (World world : getServer().getWorlds()) {
                if (world.getName().startsWith("abbacaving")) {
                    getLogger().info("Attaching giant cave populator to world \"" + world.getName() + "\"");
                    GiantCavePopulator cavePopulator = new GiantCavePopulator(this, world);
                    getServer().getPluginManager().registerEvents(cavePopulator, this);
                    world.getPopulators().add(cavePopulator);

                    int borderSize = getConfig().getInt("cave-generator.border-size");
                    getServer().dispatchCommand(getServer().getConsoleSender(), "wb shape square");
                    getServer().dispatchCommand(getServer().getConsoleSender(), "wb fillautosave 0");
                    getServer().dispatchCommand(getServer().getConsoleSender(), "wb " + world.getName() + " set " + borderSize + " " + borderSize + " 0 0");
                    getServer().dispatchCommand(getServer().getConsoleSender(), "wb " + world.getName() + " fill 500 112");
                    getServer().dispatchCommand(getServer().getConsoleSender(), "wb fill confirm");
                    break;
                }
            }

            return;
        }

        ServerMode serverMode = ServerMode.valueOf(getConfig().getString("server.mode"));
        getLogger().info("Server mode: " + serverMode.name());

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        jedis = new Jedis(
                getConfig().getString("redis.host"),
                getConfig().getInt("redis.port"));
        jedis.auth(getConfig().getString("redis.password"));

        switch (serverMode) {
            case GAME:
                initGameMode();
                break;
            case LOBBY:
                initLobbyMode();
                break;
        }

        getCommand("acreload").setExecutor((sender, command, label, args) -> {
            if (!(sender.isOp())) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                return false;
            }
            reloadConfig();
            sender.sendMessage("Reloaded configuration");
            return true;
        });

        getCommand("bcastnp").setExecutor((sender, command, label, args) -> {
            if (!(sender.isOp())) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                return false;
            }
            broadcast(String.join(" ", args));
            return true;
        });
    }

    @Override
    public void onDisable() {
        if (jedis != null) {
            String serversKey = AbbaCavingPlugin.REDIS_TAG + ":servers";
            jedis.srem(serversKey, getServerId());
            String serverList = String.join(",", jedis.smembers(serversKey));
            jedis.publish(AbbaCavingPlugin.REDIS_TAG, String.format("%s,%s", serversKey, serverList));
            getLogger().info("[Redis] Updated server list: " + serverList);

            String keyPrefix = String.format("%s:server:%s:", REDIS_TAG, getServerId());
            jedis.del(
                    keyPrefix + "state",
                    keyPrefix + "players",
                    keyPrefix + "slots",
                    keyPrefix + "counter");
            getLogger().info("[Redis] Removed data for server " + getServerId());
        }
    }

    public String getServerId() {
        return Integer.toString(getServer().getPort());
    }

    public Game getGame() {
        return game;
    }

    public Jedis getJedis() {
        return jedis;
    }

    public LuckPerms getLuckPermsApi() {
        return luckPermsApi;
    }

    public String getGameWorldName() {
        return getDescription().getName();
    }

    public CaveOre caveOreFromDrop(Material type) {
        for (CaveOre ore : ores) {
            if (ore.getDrop().getType() == type) {
                return ore;
            }
        }
        return null;
    }

    public CaveOre caveOreFromBlock(Material type) {
        for (CaveOre ore : ores) {
            if (ore.getOre() == type) {
                return ore;
            }
        }
        return null;
    }

    public CaveLoot lootFromItem(Material type) {
        for (CaveLoot lootItem : loot) {
            if (lootItem.getItemType() == type) {
                return lootItem;
            }
        }
        return null;
    }

    public boolean hasPermission(Player player, String permissionName) {
        if (player.isOp()) return true;

        if (luckPermsApi == null) {
            getLogger().warning("Not hooked into LuckPerms");
            return false;
        }

        String permissionNode = getConfig().getString("permissions." + permissionName);

        User user = luckPermsApi.getUserManager().getUser(player.getUniqueId());
        for (Node node : user.getDistinctNodes()) {
            if (node.getValue() && node.getKey().equalsIgnoreCase(permissionNode)) {
                return true;
            }
        }

        return false;
    }

    public boolean canAccess(Location loc, Location spawn) {
        int radius = getConfig().getInt("game.protected-spawn-radius");
        if (radius < 1) return true;

        return !Util.inBounds(loc,
                new Location(spawn.getWorld(),
                        spawn.getX() - radius,
                        spawn.getY() - radius,
                        spawn.getZ() - radius),
                new Location(spawn.getWorld(),
                        spawn.getX() + radius,
                        spawn.getY() + radius,
                        spawn.getZ() + radius));
    }

    public String getMessage(String name) {
        return getConfig().getString("lang." + name);
    }

    public void broadcast(String message) {
        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message));
        getLogger().info("Broadcast: \"" + message + "\"");
    }

    public void broadcast(String message, Map<String, Component> placeholders) {
        final List<TagResolver> resolvers = new ArrayList<>();

        for (final Map.Entry<String, Component> entry : placeholders.entrySet()) {
            resolvers.add(TagResolver.resolver(entry.getKey(), Tag.inserting(entry.getValue())));
        }

        this.broadcast(message, resolvers.toArray(new TagResolver[]{}));
    }

    public void broadcast(String message, TagResolver... placeholders) {
        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message, placeholders));
    }

    public void message(CommandSender sender, String message) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public void message(CommandSender sender, String message, Map<String, String> placeholders) {
        final List<TagResolver> resolvers = new ArrayList<>();

        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers.add(TagResolver.resolver(entry.getKey(), Tag.inserting(Component.text(entry.getValue()))));
        }

        this.message(sender, message, resolvers.toArray(new TagResolver[]{}));
    }

    public void message(CommandSender sender, String message, TagResolver... placeholders) {
        final TagResolver name = TagResolver.resolver("name", Tag.inserting(Component.text(sender.getName())));
        final TagResolver resolvers = TagResolver.resolver(TagResolver.resolver(placeholders), name);

        sender.sendMessage(MiniMessage.miniMessage().deserialize(message, resolvers));
    }

    public void updateGameInfo(String subKey, String value) {
        if (jedis == null) {
            getLogger().warning("Not currently connected to a Redis instance");
            return;
        }

        String key = String.format("%s:server:%s:%s", REDIS_TAG, getServerId(), subKey);
        jedis.set(key, value);
        jedis.publish(REDIS_TAG, String.format("%s,%s", key, value));
//        getLogger().info(String.format("[Redis] Set %s = %s", key, value));
    }

    private void initGameMode() {
        loadData();

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this),this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this),this);
        getServer().getPluginManager().registerEvents(new EntityListener(this),this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this),this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this),this);

        getServer().getScheduler().runTaskAsynchronously(this, this::createTable);

        registerGamePlaceholders();

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPermsApi = provider.getProvider();
            getLogger().info("Hooked into " + provider.getPlugin().getName());
        }

        getCommand("nightvision").setExecutor(new NightVisionCommand());
        getCommand("forcestart").setExecutor(new ForceStartCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("points").setExecutor(new PointsCommand(this));

        game = new Game(this, mapSpawns);
        dataSource = initDataSource();
    }

    public Set<CaveOre> getOres() {
        return ores;
    }

    private Map<String, ServerInfo> servers;

    public Map<String, ServerInfo> getServers() {
        return servers;
    }

    public static final class ServerInfo {
        public GameState state;
        public int playerCount, counter, slots;
    }

    private ServerInfo getServerInfo(String serverId) {
        ServerInfo info = servers.get(serverId);
        if (info == null) {
            info = new ServerInfo();
            servers.put(serverId, info);
        }
        return info;
    }

    private void initLobbyMode() {
        servers = new HashMap<>();

        Set<String> serverIds = jedis.smembers(REDIS_TAG + ":servers");
        getLogger().info("[Redis] Loaded server list: " + serverIds.toString());

        for (String serverId : serverIds) {
            ServerInfo info = getServerInfo(serverId);

            String stateValue = jedis.get(String.format("%s:server:%s:state", REDIS_TAG, serverId));
            if (stateValue != null) {
                info.state = GameState.valueOf(stateValue);
            } else {
                getLogger().warning("[Redis] No state entry found for server %s" + serverId);
            }

            String playerCountValue = jedis.get(String.format("%s:server:%s:players", REDIS_TAG, serverId));
            if (playerCountValue != null) {
                info.playerCount = Integer.parseInt(playerCountValue);
            } else {
                getLogger().warning("[Redis] No player count entry found for server %s" + serverId);
            }

            String counterValue = jedis.get(String.format("%s:server:%s:counter", REDIS_TAG, serverId));
            if (counterValue != null) {
                info.counter = Integer.parseInt(counterValue);
            } else {
                getLogger().warning("[Redis] No counter entry found for server %s" + serverId);
            }

            String slotsValue = jedis.get(String.format("%s:server:%s:slots", REDIS_TAG, serverId));
            if (slotsValue != null) {
                info.slots = Integer.parseInt(slotsValue);
            } else {
                getLogger().warning("[Redis] No slots entry found for server %s" + serverId);
            }

            getLogger().info(String.format("[Redis] Loaded server info: id=%s, state=%s, players=%s, counter=%s, slots=%s",
                    serverId, stateValue, playerCountValue, counterValue, slotsValue));
        }

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (!channel.equals(REDIS_TAG)) return;

                        String[] parts = message.split(",", 2);
                        String key = parts[0], value = parts[1];
//                getLogger().info(String.format("[Redis] Received update: key=%s, value=%s", key, value));

                        if (key.endsWith(":servers")) {
                            Set<String> newServerIds = new HashSet<>(Arrays.asList(value.split(",")));

                            Iterator<Entry<String, ServerInfo>> itr = servers.entrySet().iterator();
                            while (itr.hasNext()) {
                                String entryKey = itr.next().getKey();
                                if (!newServerIds.contains(entryKey)) {
                                    itr.remove();
                                }
                            }

                            for (String serverId : newServerIds) {
                                if (!servers.containsKey(serverId)) {
                                    servers.put(serverId, new ServerInfo());

                                    if (Bukkit.getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")) {
                                        registerServerPlaceholders(serverId);
                                    }
                                }
                            }

//                    getLogger().info("[Redis] Updated server list: " + newServerIds.toString());
                            return;
                        }

                        String[] keyParts = key.split(":");
                        String serverId = keyParts[2], attributeName = keyParts[3];

                        ServerInfo info = getServerInfo(serverId);
                        switch (attributeName) {
                            case "state":
                                info.state = GameState.valueOf(value);
                                break;
                            case "players":
                                info.playerCount = Integer.parseInt(value);
                                break;
                            case "counter":
                                info.counter = Integer.parseInt(value);
                                break;
                            case "slots":
                                info.slots = Integer.parseInt(value);
                                break;
                            default:
                                getLogger().warning("[Redis] Unknown attribute name: " + attributeName);
                                break;
                        }
                    }
                }, REDIS_TAG);
            }
        });

        registerLobbyPlaceholders();
    }

    private void registerGamePlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholdersAPI")) {
            new GamePlaceholders(this);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")) {
            PlaceholderAPI.registerPlaceholder(this, "x", event ->
                Integer.toString(event.getPlayer().getLocation().getBlockX()));
            PlaceholderAPI.registerPlaceholder(this, "y", event ->
                    Integer.toString(event.getPlayer().getLocation().getBlockY()));
            PlaceholderAPI.registerPlaceholder(this, "z", event ->
                    Integer.toString(event.getPlayer().getLocation().getBlockZ()));
            PlaceholderAPI.registerPlaceholder(this, "current_score", event -> {
                GamePlayer gp = game.getPlayer(event.getPlayer());
                if (gp != null) {
                    return Util.addCommas(gp.getScore());
                }
                return "";
            });
            PlaceholderAPI.registerPlaceholder(this, "highest_score", event -> {
                GamePlayer gp = game.getPlayer(event.getPlayer());
                if (gp != null) {
                    return Util.addCommas(gp.getHighestScore());
                }
                return "";
            });
            PlaceholderAPI.registerPlaceholder(this, "current_ores_mined", event -> {
                GamePlayer gp = game.getPlayer(event.getPlayer());
                if (gp != null) {
                    return Util.addCommas(gp.getCurrentOresMined());
                }
                return "";
            });
            PlaceholderAPI.registerPlaceholder(this, "total_ores_mined", event -> {
                GamePlayer gp = game.getPlayer(event.getPlayer());
                if (gp != null) {
                    return Util.addCommas(gp.getTotalOresMined());
                }
                return "";
            });
            PlaceholderAPI.registerPlaceholder(this, "wins", event -> {
                GamePlayer gp = game.getPlayer(event.getPlayer());
                if (gp != null) {
                    return Util.addCommas(gp.getWins());
                }
                return "";
            });
            PlaceholderAPI.registerPlaceholder(this, "game_players", event -> Integer.toString(game.getPlayers().size()));
            PlaceholderAPI.registerPlaceholder(this, "game_state", event -> game.getState().getDisplayName());

            for (int i = 0; i < 10; i++) {
                final int index = i;
                PlaceholderAPI.registerPlaceholder(this, "leaderboard_score_" + (i + 1), event -> {
                    if (game.getLeaderboard() != null) {
                        List<GamePlayer> sorted = new ArrayList<>(game.getLeaderboard().keySet());
                        if (index >= sorted.size()) return "N/A";
                        return Util.addCommas(game.getLeaderboard().get(sorted.get(index)));
                    }
                    return "";
                });
                PlaceholderAPI.registerPlaceholder(this, "leaderboard_player_" + (i + 1), event -> {
                    if (game.getLeaderboard() != null) {
                        List<GamePlayer> sorted = new ArrayList<>(game.getLeaderboard().keySet());
                        if (index >= sorted.size()) return "N/A";
                        return sorted.get(index).getPlayer().getName();
                    }
                    return "";
                });
            }
        }
    }

    private void registerServerPlaceholders(String serverId) {
        String prefix = String.format("%s_server_%s_", REDIS_TAG, serverId);

        PlaceholderAPI.registerPlaceholder(this, prefix + "state", event -> {
            ServerInfo info = servers.get(serverId);
            return info != null && info.state != null ? info.state.getDisplayName() : "";
        });
        PlaceholderAPI.registerPlaceholder(this, prefix + "players", event -> {
            ServerInfo info = servers.get(serverId);
            return info != null ? Integer.toString(info.playerCount) : "";
        });
        PlaceholderAPI.registerPlaceholder(this, prefix + "counter", event -> {
            ServerInfo info = servers.get(serverId);
            return info != null ? Integer.toString(info.counter) : "";
        });
        PlaceholderAPI.registerPlaceholder(this, prefix + "slots", event -> {
            ServerInfo info = servers.get(serverId);
            return info != null ? Integer.toString(info.slots) : "";
        });
    }

    private void registerLobbyPlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholdersAPI")) {
            new LobbyPlaceholders(this);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")) {
            PlaceholderAPI.registerPlaceholder(this,"abbacaving_online", event -> {
                int totalOnline = 0;
                if (servers != null) {
                    for (ServerInfo info : servers.values()) {
                        totalOnline += info.playerCount;
                    }
                }
                return Integer.toString(totalOnline);
            });

            for (String serverId : servers.keySet()) {
                registerServerPlaceholders(serverId);
            }
        }
    }

    private void loadData() {
        ores = new HashSet<>();
        for (Map<?, ?> entry : getConfig().getMapList("game.ores")) {
            Map<?, ?> value = (Map<?, ?>) entry.get("value");
            ores.add(new CaveOre(
                    (String) entry.get("name"),
                    (Integer) value.get("exact"),
                    (Integer) value.get("min"),
                    (Integer) value.get("max"),
                    (Double) value.get("probability"),
                    Material.valueOf((String) entry.get("block")),
                    new ItemStack(Material.valueOf((String) entry.get("drop")))
            ));
        }
        ores = ores.stream()
                .sorted((o1, o2) -> Integer.compare(o2.getValue(), o1.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        getLogger().info("Loaded " + ores.size() + " ore(s)");

        loot = new HashSet<>();
        for (Map<?, ?> entry : getConfig().getMapList("game.loot-items")) {
            Map<?, ?> value = (Map<?, ?>) entry.get("value");
            loot.add(new CaveLoot(
                    (String) entry.get("name"),
                    (String) entry.get("article"),
                    (Integer) value.get("exact"),
                    (Integer) value.get("min"),
                    (Integer) value.get("max"),
                    Material.valueOf((String) entry.get("item"))
            ));
        }
        getLogger().info("Loaded " + loot.size() + " loot item(s)");

        mapSpawns = new HashMap<>();

        FileConfiguration spawnsConfig = new YamlConfiguration();
        try {
            String spawnsAbsPath = getConfig().getString("spawns-file", "");
            File spawnsFile;
            if (!spawnsAbsPath.isEmpty()) {
                spawnsFile = new File(spawnsAbsPath);
            } else {
                spawnsFile = new File(getDataFolder(), "spawns.yml");
            }

            spawnsConfig.load(spawnsFile);
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("Failed to load spawns.yml");
            e.printStackTrace();
            return;
        }

        for (String entry : spawnsConfig.getKeys(false)) {
            List<Location> spawns = new ArrayList<>();
            for (String location : spawnsConfig.getStringList(entry)) {
                String[] split = location.split(",");
                spawns.add(new Location(
                        null,
                        Double.parseDouble(split[0]),
                        Double.parseDouble(split[1]),
                        Double.parseDouble(split[2])
                ));
            }
            mapSpawns.put(entry, spawns);
        }
        getLogger().info("Loaded spawns for " + mapSpawns.size() + " map(s)");
    }

    private Jedis createRedisConnection() {
        jedis = new Jedis(
                getConfig().getString("redis.host"),
                getConfig().getInt("redis.port"));
        jedis.auth(getConfig().getString("redis.password"));
        return jedis;
    }

    private HikariDataSource initDataSource() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String host = getConfig().getString("database.host");
        String name = getConfig().getString("database.name");

        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(4);
        config.setJdbcUrl("jdbc:mariadb://" + host + "/" + name);
        config.setUsername(getConfig().getString("database.user"));
        config.setPassword(getConfig().getString("database.password"));
        return new HikariDataSource(config);
    }

    private void createTable() {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS abba_caving_stats (uuid VARCHAR(50) PRIMARY KEY, wins INT, highest_score INT, ores_mined INT);");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void loadPlayerStats(GamePlayer gp) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT wins, highest_score, ores_mined FROM abba_caving_stats WHERE uuid = ?;")) {
                stmt.setString(1, gp.getPlayer().getUniqueId().toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        gp.setWins(rs.getInt("wins"));
                        gp.setHighestScore(rs.getInt("highest_score"));
                        gp.setTotalOresMined(rs.getInt("ores_mined"));
                        getLogger().info("Loaded " + gp.getPlayer().getName() + "'s stats");
                    } else {
                        getLogger().info("No stats found for player " + gp.getPlayer().getName());
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void savePlayerStats(GamePlayer gp) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO abba_caving_stats (uuid, wins, highest_score, ores_mined) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE wins = ?, highest_score = ?, ores_mined = ?;")) {
                stmt.setString(1, gp.getPlayer().getUniqueId().toString());
                stmt.setInt(2, gp.getWins());
                stmt.setInt(3, gp.getHighestScore());
                stmt.setInt(4, gp.getTotalOresMined());
                stmt.setInt(5, gp.getWins());
                stmt.setInt(6, gp.getHighestScore());
                stmt.setInt(7, gp.getTotalOresMined());
                stmt.executeUpdate();
                getLogger().info("Saved " + gp.getPlayer().getName() + "'s stats");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}

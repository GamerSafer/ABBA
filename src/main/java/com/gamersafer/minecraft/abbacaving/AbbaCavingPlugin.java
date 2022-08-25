package com.gamersafer.minecraft.abbacaving;

import com.gamersafer.minecraft.abbacaving.commands.ForceStartCommand;
import com.gamersafer.minecraft.abbacaving.commands.NightVisionCommand;
import com.gamersafer.minecraft.abbacaving.commands.PointsCommand;
import com.gamersafer.minecraft.abbacaving.commands.StatsCommand;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.listeners.BlockBreakListener;
import com.gamersafer.minecraft.abbacaving.listeners.BlockPlaceListener;
import com.gamersafer.minecraft.abbacaving.listeners.EntityListener;
import com.gamersafer.minecraft.abbacaving.listeners.InventoryListener;
import com.gamersafer.minecraft.abbacaving.listeners.PlayerListener;
import com.gamersafer.minecraft.abbacaving.placeholders.GamePlaceholders;
import com.gamersafer.minecraft.abbacaving.placeholders.LobbyPlaceholders;
import com.gamersafer.minecraft.abbacaving.util.Util;
import com.gamersafer.minecraft.abbacaving.worldgen.GiantCavePopulator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
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

public class AbbaCavingPlugin extends JavaPlugin {

    public static final String REDIS_TAG = "abbacaving";
    private Set<CaveOre> ores;
    private Set<CaveLoot> loot;
    private Map<String, List<Location>> mapSpawns;
    private Game game;
    private Jedis jedis;
    private LuckPerms luckPermsApi;
    private HikariDataSource dataSource;
    private Map<String, ServerInfo> servers;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        if (this.getConfig().getBoolean("cave-generator.generator-mode")) {
            this.getLogger().info("Generator mode");

            for (final World world : this.getServer().getWorlds()) {
                if (world.getName().startsWith("abbacaving")) {
                    this.getLogger().info("Attaching giant cave populator to world \"" + world.getName() + "\"");
                    final GiantCavePopulator cavePopulator = new GiantCavePopulator(this, world);
                    this.getServer().getPluginManager().registerEvents(cavePopulator, this);
                    world.getPopulators().add(cavePopulator);

                    final int borderSize = this.getConfig().getInt("cave-generator.border-size");
                    this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "wb shape square");
                    this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "wb fillautosave 0");
                    this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "wb " + world.getName() + " set " + borderSize + " " + borderSize + " 0 0");
                    this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "wb " + world.getName() + " fill 500 112");
                    this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "wb fill confirm");
                    break;
                }
            }

            return;
        }

        final ServerMode serverMode = ServerMode.valueOf(this.getConfig().getString("server.mode"));
        this.getLogger().info("Server mode: " + serverMode.name());

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        this.jedis = new Jedis(
                this.getConfig().getString("redis.host"),
                this.getConfig().getInt("redis.port"));
        this.jedis.auth(this.getConfig().getString("redis.password"));

        switch (serverMode) {
            case GAME -> this.initGameMode();
            case LOBBY -> this.initLobbyMode();
        }

        this.getCommand("acreload").setExecutor((sender, command, label, args) -> {
            if (!(sender.isOp())) {
                this.message(sender, "<red>You do not have permission to do this.");
                return false;
            }
            this.reloadConfig();
            sender.sendMessage("Reloaded configuration");
            return true;
        });

        this.getCommand("bcastnp").setExecutor((sender, command, label, args) -> {
            if (!(sender.isOp())) {
                this.message(sender, "<red>You do not have permission to do this.");
                return false;
            }
            this.broadcast(String.join(" ", args));
            return true;
        });
    }

    @Override
    public void onDisable() {
        if (this.jedis != null) {
            final String serversKey = AbbaCavingPlugin.REDIS_TAG + ":servers";
            this.jedis.srem(serversKey, this.serverId());
            final String serverList = String.join(",", this.jedis.smembers(serversKey));
            this.jedis.publish(AbbaCavingPlugin.REDIS_TAG, String.format("%s,%s", serversKey, serverList));
            this.getLogger().info("[Redis] Updated server list: " + serverList);

            final String keyPrefix = String.format("%s:server:%s:", REDIS_TAG, this.serverId());
            this.jedis.del(
                    keyPrefix + "state",
                    keyPrefix + "players",
                    keyPrefix + "slots",
                    keyPrefix + "counter");
            this.getLogger().info("[Redis] Removed data for server " + this.serverId());
        }
    }

    public String serverId() {
        return Integer.toString(this.getServer().getPort());
    }

    public Game currentGame() {
        return this.game;
    }

    public Jedis jedis() {
        return this.jedis;
    }

    public LuckPerms luckPermsAPI() {
        return this.luckPermsApi;
    }

    public String gameWorldName() {
        return this.getDescription().getName();
    }

    public CaveOre caveOreFromDrop(final Material type) {
        for (final CaveOre ore : this.ores) {
            if (ore.itemDrop().getType() == type) {
                return ore;
            }
        }
        return null;
    }

    public CaveOre caveOreFromBlock(final Material type) {
        for (final CaveOre ore : this.ores) {
            if (ore.ore() == type) {
                return ore;
            }
        }
        return null;
    }

    public CaveLoot lootFromItem(final Material type) {
        for (final CaveLoot lootItem : this.loot) {
            if (lootItem.itemType() == type) {
                return lootItem;
            }
        }
        return null;
    }

    public boolean hasPermission(final Player player, final String permissionName) {
        if (player.isOp()) return true;

        if (this.luckPermsApi == null) {
            this.getLogger().warning("Not hooked into LuckPerms");
            return false;
        }

        final String permissionNode = this.getConfig().getString("permissions." + permissionName);

        final User user = this.luckPermsApi.getUserManager().getUser(player.getUniqueId());
        for (final Node node : user.getDistinctNodes()) {
            if (node.getValue() && node.getKey().equalsIgnoreCase(permissionNode)) {
                return true;
            }
        }

        return false;
    }

    public boolean canAccess(final Location loc, final Location spawn) {
        final int radius = this.getConfig().getInt("game.protected-spawn-radius");
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

    public String configMessage(final String name) {
        return this.getConfig().getString("lang." + name);
    }

    public void broadcast(final String message) {
        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message));
        this.getLogger().info("Broadcast: \"" + message + "\"");
    }

    public void broadcast(final String message, final Map<String, Component> placeholders) {
        final List<TagResolver> resolvers = new ArrayList<>();

        for (final Map.Entry<String, Component> entry : placeholders.entrySet()) {
            resolvers.add(TagResolver.resolver(entry.getKey(), Tag.inserting(entry.getValue())));
        }

        this.broadcast(message, resolvers.toArray(new TagResolver[]{}));
    }

    public void broadcast(final String message, final TagResolver... placeholders) {
        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message, placeholders));
    }

    public void message(final CommandSender sender, final String message) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public void message(final CommandSender sender, final String message, final Map<String, String> placeholders) {
        final List<TagResolver> resolvers = new ArrayList<>();

        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers.add(TagResolver.resolver(entry.getKey(), Tag.inserting(Component.text(entry.getValue()))));
        }

        this.message(sender, message, resolvers.toArray(new TagResolver[]{}));
    }

    public void message(final CommandSender sender, final String message, final TagResolver... placeholders) {
        final TagResolver name = TagResolver.resolver("name", Tag.inserting(Component.text(sender.getName())));
        final TagResolver resolvers = TagResolver.resolver(TagResolver.resolver(placeholders), name);

        sender.sendMessage(MiniMessage.miniMessage().deserialize(message, resolvers));
    }

    public void updateGameInfo(final String subKey, final String value) {
        if (this.jedis == null) {
            this.getLogger().warning("Not currently connected to a Redis instance");
            return;
        }

        final String key = String.format("%s:server:%s:%s", REDIS_TAG, this.serverId(), subKey);
        this.jedis.set(key, value);
        this.jedis.publish(REDIS_TAG, String.format("%s,%s", key, value));
        //getLogger().info(String.format("[Redis] Set %s = %s", key, value));
    }

    private void initGameMode() {
        this.loadData();

        this.getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        this.getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        this.getServer().getScheduler().runTaskAsynchronously(this, this::createTable);

        this.registerGamePlaceholders();

        final RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPermsApi = provider.getProvider();
            this.getLogger().info("Hooked into " + provider.getPlugin().getName());
        }

        this.getCommand("nightvision").setExecutor(new NightVisionCommand());
        this.getCommand("forcestart").setExecutor(new ForceStartCommand(this));
        this.getCommand("stats").setExecutor(new StatsCommand(this));
        this.getCommand("points").setExecutor(new PointsCommand(this));

        this.game = new Game(this, this.mapSpawns);
        this.dataSource = this.initDataSource();
    }

    public Set<CaveOre> ores() {
        return this.ores;
    }

    public Map<String, ServerInfo> servers() {
        return this.servers;
    }

    private ServerInfo serverInfo(final String serverId) {
        ServerInfo info = this.servers.get(serverId);
        if (info == null) {
            info = new ServerInfo();
            this.servers.put(serverId, info);
        }
        return info;
    }

    private void initLobbyMode() {
        this.servers = new HashMap<>();

        final Set<String> serverIds = this.jedis.smembers(REDIS_TAG + ":servers");
        this.getLogger().info("[Redis] Loaded server list: " + serverIds.toString());

        for (final String serverId : serverIds) {
            final ServerInfo info = this.serverInfo(serverId);

            final String stateValue = this.jedis.get(String.format("%s:server:%s:state", REDIS_TAG, serverId));
            if (stateValue != null) {
                info.state = GameState.valueOf(stateValue);
            } else {
                this.getLogger().warning("[Redis] No state entry found for server %s" + serverId);
            }

            final String playerCountValue = this.jedis.get(String.format("%s:server:%s:players", REDIS_TAG, serverId));
            if (playerCountValue != null) {
                info.playerCount = Integer.parseInt(playerCountValue);
            } else {
                this.getLogger().warning("[Redis] No player count entry found for server %s" + serverId);
            }

            final String counterValue = this.jedis.get(String.format("%s:server:%s:counter", REDIS_TAG, serverId));
            if (counterValue != null) {
                info.counter = Integer.parseInt(counterValue);
            } else {
                this.getLogger().warning("[Redis] No counter entry found for server %s" + serverId);
            }

            final String slotsValue = this.jedis.get(String.format("%s:server:%s:slots", REDIS_TAG, serverId));
            if (slotsValue != null) {
                info.slots = Integer.parseInt(slotsValue);
            } else {
                this.getLogger().warning("[Redis] No slots entry found for server %s" + serverId);
            }

            this.getLogger().info(String.format("[Redis] Loaded server info: id=%s, state=%s, players=%s, counter=%s, slots=%s",
                    serverId, stateValue, playerCountValue, counterValue, slotsValue));
        }

        Executors.newSingleThreadExecutor().execute(() -> AbbaCavingPlugin.this.jedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(final String channel, final String message) {
                if (!channel.equals(REDIS_TAG)) return;

                final String[] parts = message.split(",", 2);
                final String key = parts[0];
                final String value = parts[1];
                //getLogger().info(String.format("[Redis] Received update: key=%s, value=%s", key, value));

                if (key.endsWith(":servers")) {
                    final Set<String> newServerIds = new HashSet<>(Arrays.asList(value.split(",")));

                    final Iterator<Entry<String, ServerInfo>> itr = AbbaCavingPlugin.this.servers.entrySet().iterator();
                    while (itr.hasNext()) {
                        final String entryKey = itr.next().getKey();
                        if (!newServerIds.contains(entryKey)) {
                            itr.remove();
                        }
                    }

                    for (final String serverId : newServerIds) {
                        if (!AbbaCavingPlugin.this.servers.containsKey(serverId)) {
                            AbbaCavingPlugin.this.servers.put(serverId, new ServerInfo());
                        }
                    }

                    //getLogger().info("[Redis] Updated server list: " + newServerIds.toString());
                    return;
                }

                final String[] keyParts = key.split(":");
                final String serverId = keyParts[2];
                final String attributeName = keyParts[3];

                final ServerInfo info = AbbaCavingPlugin.this.serverInfo(serverId);
                switch (attributeName) {
                    case "state" -> info.state = GameState.valueOf(value);
                    case "players" -> info.playerCount = Integer.parseInt(value);
                    case "counter" -> info.counter = Integer.parseInt(value);
                    case "slots" -> info.slots = Integer.parseInt(value);
                    default ->
                            AbbaCavingPlugin.this.getLogger().warning("[Redis] Unknown attribute name: " + attributeName);
                }
            }
        }, REDIS_TAG));

        this.registerLobbyPlaceholders();
    }

    private void registerGamePlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholdersAPI")) {
            new GamePlaceholders(this);
        }
    }

    private void registerLobbyPlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholdersAPI")) {
            new LobbyPlaceholders(this);
        }
    }

    private void loadData() {
        this.ores = new HashSet<>();
        for (final Map<?, ?> entry : this.getConfig().getMapList("game.ores")) {
            final Map<?, ?> value = (Map<?, ?>) entry.get("value");
            this.ores.add(new CaveOre(
                    (String) entry.get("name"),
                    (Integer) value.get("exact"),
                    (Integer) value.get("min"),
                    (Integer) value.get("max"),
                    (Double) value.get("probability"),
                    Material.valueOf((String) entry.get("block")),
                    new ItemStack(Material.valueOf((String) entry.get("drop")))
            ));
        }
        this.ores = this.ores.stream()
                .sorted((o1, o2) -> Integer.compare(o2.value(), o1.value()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.getLogger().info("Loaded " + this.ores.size() + " ore(s)");

        this.loot = new HashSet<>();
        for (final Map<?, ?> entry : this.getConfig().getMapList("game.loot-items")) {
            final Map<?, ?> value = (Map<?, ?>) entry.get("value");
            this.loot.add(new CaveLoot(
                    (String) entry.get("name"),
                    (String) entry.get("article"),
                    (Integer) value.get("exact"),
                    (Integer) value.get("min"),
                    (Integer) value.get("max"),
                    Material.valueOf((String) entry.get("item"))
            ));
        }
        this.getLogger().info("Loaded " + this.loot.size() + " loot item(s)");

        this.mapSpawns = new HashMap<>();

        final FileConfiguration spawnsConfig = new YamlConfiguration();
        try {
            final String spawnsAbsPath = this.getConfig().getString("spawns-file", "");
            final File spawnsFile;
            if (!spawnsAbsPath.isEmpty()) {
                spawnsFile = new File(spawnsAbsPath);
            } else {
                spawnsFile = new File(this.getDataFolder(), "spawns.yml");
            }

            spawnsConfig.load(spawnsFile);
        } catch (final IOException | InvalidConfigurationException e) {
            this.getLogger().warning("Failed to load spawns.yml");
            e.printStackTrace();
            return;
        }

        for (final String entry : spawnsConfig.getKeys(false)) {
            final List<Location> spawns = new ArrayList<>();
            for (final String location : spawnsConfig.getStringList(entry)) {
                final String[] split = location.split(",");
                spawns.add(new Location(
                        null,
                        Double.parseDouble(split[0]),
                        Double.parseDouble(split[1]),
                        Double.parseDouble(split[2])
                ));
            }
            this.mapSpawns.put(entry, spawns);
        }
        this.getLogger().info("Loaded spawns for " + this.mapSpawns.size() + " map(s)");
    }

    private Jedis createRedisConnection() {
        this.jedis = new Jedis(
                this.getConfig().getString("redis.host"),
                this.getConfig().getInt("redis.port"));
        this.jedis.auth(this.getConfig().getString("redis.password"));
        return this.jedis;
    }

    private HikariDataSource initDataSource() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }

        final String host = this.getConfig().getString("database.host");
        final String name = this.getConfig().getString("database.name");

        final HikariConfig config = new HikariConfig();
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(4);
        config.setJdbcUrl("jdbc:mariadb://" + host + "/" + name);
        config.setUsername(this.getConfig().getString("database.user"));
        config.setPassword(this.getConfig().getString("database.password"));
        return new HikariDataSource(config);
    }

    private void createTable() {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS abba_caving_stats (uuid VARCHAR(50) PRIMARY KEY, wins INT, highest_score INT, ores_mined INT);");
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void loadPlayerStats(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final PreparedStatement stmt = conn.prepareStatement("SELECT wins, highest_score, ores_mined FROM abba_caving_stats WHERE uuid = ?;")) {
                stmt.setString(1, gp.player().getUniqueId().toString());

                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        gp.wins(rs.getInt("wins"));
                        gp.highestScore(rs.getInt("highest_score"));
                        gp.totalOresMined(rs.getInt("ores_mined"));
                        this.getLogger().info("Loaded " + gp.player().getName() + "'s stats");
                    } else {
                        this.getLogger().info("No stats found for player " + gp.player().getName());
                    }
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void savePlayerStats(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO abba_caving_stats (uuid, wins, highest_score, ores_mined) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE wins = ?, highest_score = ?, ores_mined = ?;")) {
                stmt.setString(1, gp.player().getUniqueId().toString());
                stmt.setInt(2, gp.wins());
                stmt.setInt(3, gp.highestScore());
                stmt.setInt(4, gp.totalOresMined());
                stmt.setInt(5, gp.wins());
                stmt.setInt(6, gp.highestScore());
                stmt.setInt(7, gp.totalOresMined());
                stmt.executeUpdate();
                this.getLogger().info("Saved " + gp.player().getName() + "'s stats");
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    private enum ServerMode {
        GAME, LOBBY
    }

    public static final class ServerInfo {
        public GameState state;
        public int playerCount;
        public int counter;
        public int slots;
    }

}

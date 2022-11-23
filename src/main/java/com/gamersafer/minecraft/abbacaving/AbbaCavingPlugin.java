package com.gamersafer.minecraft.abbacaving;

import com.gamersafer.minecraft.abbacaving.commands.ACLookupCommand;
import com.gamersafer.minecraft.abbacaving.commands.ACReloadCommand;
import com.gamersafer.minecraft.abbacaving.commands.BroadcastNPCommand;
import com.gamersafer.minecraft.abbacaving.commands.ForceStartCommand;
import com.gamersafer.minecraft.abbacaving.commands.JoinCommand;
import com.gamersafer.minecraft.abbacaving.commands.LeaveCommand;
import com.gamersafer.minecraft.abbacaving.commands.NightVisionCommand;
import com.gamersafer.minecraft.abbacaving.commands.PointsCommand;
import com.gamersafer.minecraft.abbacaving.commands.StatsCommand;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameTracker;
import com.gamersafer.minecraft.abbacaving.listeners.BlockBreakListener;
import com.gamersafer.minecraft.abbacaving.listeners.BlockPlaceListener;
import com.gamersafer.minecraft.abbacaving.listeners.EntityListener;
import com.gamersafer.minecraft.abbacaving.listeners.InventoryListener;
import com.gamersafer.minecraft.abbacaving.listeners.PlayerListener;
import com.gamersafer.minecraft.abbacaving.lobby.Lobby;
import com.gamersafer.minecraft.abbacaving.placeholders.GamePlaceholders;
import com.gamersafer.minecraft.abbacaving.placeholders.LobbyPlaceholders;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class AbbaCavingPlugin extends JavaPlugin {

    private Set<CaveOre> ores;
    private Set<CaveLoot> loot;
    private GameTracker gameTracker;
    private Lobby lobby;
    private Map<String, List<Location>> mapSpawns;
    private HikariDataSource dataSource;
    private FileConfiguration mapsConfig = new YamlConfiguration();
    private FileConfiguration messagesConfig = new YamlConfiguration();
    private FileConfiguration pointsConfig = new YamlConfiguration();
    private final Map<String, Game> maps = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.messagesConfig = this.fileConfiguration("messages.yml");
        this.pointsConfig = this.fileConfiguration("points.yml");

        final File schematicDirectory = new File(this.getDataFolder(), "schematics");

        if (!schematicDirectory.exists()) {
            schematicDirectory.mkdirs();
        }

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        this.loadData();

        this.getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        this.getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        this.getServer().getScheduler().runTaskAsynchronously(this, this::createTable);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholdersAPI")) {
            new GamePlaceholders(this);
            new LobbyPlaceholders(this);
        }

        try {
            this.dataSource = this.initDataSource();
        } catch (final ClassNotFoundException | IllegalArgumentException | IllegalStateException exception) {
            throw new RuntimeException(exception);
        }

        this.getCommand("aclookup").setExecutor(new ACLookupCommand(this));
        this.getCommand("acreload").setExecutor(new ACReloadCommand(this));
        this.getCommand("bcastnp").setExecutor(new BroadcastNPCommand(this));
        this.getCommand("forcestart").setExecutor(new ForceStartCommand(this));
        this.getCommand("join").setExecutor(new JoinCommand(this));
        this.getCommand("leave").setExecutor(new LeaveCommand(this));
        this.getCommand("nightvision").setExecutor(new NightVisionCommand(this));
        this.getCommand("points").setExecutor(new PointsCommand(this));
        this.getCommand("stats").setExecutor(new StatsCommand(this));

        for (final String mapName : this.configuredMapNames()) {
            this.maps.put(mapName, new Game(this, mapName));
        }

        this.gameTracker = new GameTracker(this);
        this.lobby = new Lobby(this);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        this.mapsConfig = this.fileConfiguration("maps.yml");
        this.messagesConfig = this.fileConfiguration("messages.yml");
        this.pointsConfig = this.fileConfiguration("points.yml");
    }

    public Game game(final String mapName) {
        return this.maps.get(mapName);
    }

    private FileConfiguration fileConfiguration(final String fileName) {
        final FileConfiguration configuration = new YamlConfiguration();

        try {
            final File file = new File(this.getDataFolder(), fileName);

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                this.saveResource(fileName, false);
            }

            configuration.load(file);
        } catch (final IOException | InvalidConfigurationException exception) {
            this.getLogger().warning("Failed to load " + fileName);
            exception.printStackTrace();
        }

        return configuration;
    }

    public GameTracker gameTracker() {
        return this.gameTracker;
    }

    public Lobby lobby() {
        return this.lobby;
    }

    public Map<String, List<Location>> mapSpawns() {
        return this.mapSpawns;
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

    public String configMessage(final String name) {
        return this.messagesConfig.getString(name);
    }

    public List<String> configuredMapNames() {
        final List<String> mapNames = new ArrayList<>();

        for (final String key : this.mapsConfig.getKeys(false)) {
            if (!key.equals("default-settings")) {
                mapNames.add(key);
            }
        }

        return mapNames;
    }

    public ConfigurationSection mapSettings(final String mapName) {
        final ConfigurationSection section = this.mapsConfig.getConfigurationSection(mapName);

        if (section != null) {
            return section;
        }

        return this.mapsConfig.getConfigurationSection("default-settings");
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

    public Set<CaveOre> ores() {
        return this.ores;
    }

    private void loadData() {
        this.ores = new HashSet<>();

        for (final Map<?, ?> entry : this.pointsConfig.getMapList("ores")) {
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

        for (final Map<?, ?> entry : this.pointsConfig.getMapList("loot-items")) {
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
    }

    private HikariDataSource initDataSource() throws ClassNotFoundException, IllegalStateException, IllegalArgumentException {
        Class.forName("org.mariadb.jdbc.Driver");

        final String host = this.getConfig().getString("mysql.host");
        final String name = this.getConfig().getString("mysql.database-name");

        final HikariConfig config = new HikariConfig();
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(4);
        config.setJdbcUrl("jdbc:mariadb://" + host + "/" + name);
        config.setUsername(this.getConfig().getString("mysql.username"));
        config.setPassword(this.getConfig().getString("mysql.password"));
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

}

package com.gamersafer.minecraft.abbacaving;

import com.gamersafer.minecraft.abbacaving.commands.ACReloadCommand;
import com.gamersafer.minecraft.abbacaving.commands.BroadcastNPCommand;
import com.gamersafer.minecraft.abbacaving.commands.ForceStartCommand;
import com.gamersafer.minecraft.abbacaving.commands.NightVisionCommand;
import com.gamersafer.minecraft.abbacaving.commands.PointsCommand;
import com.gamersafer.minecraft.abbacaving.commands.StatsCommand;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameTracker;
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

public class AbbaCavingPlugin extends JavaPlugin {

    private Set<CaveOre> ores;
    private Set<CaveLoot> loot;
    private GameTracker gameTracker;
    private Map<String, List<Location>> mapSpawns;
    private LuckPerms luckPermsApi;
    private HikariDataSource dataSource;

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

        final RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPermsApi = provider.getProvider();
            this.getLogger().info("Hooked into " + provider.getPlugin().getName());
        }

        this.dataSource = this.initDataSource();

        this.getCommand("acreload").setExecutor(new ACReloadCommand(this));
        this.getCommand("bcastnp").setExecutor(new BroadcastNPCommand(this));
        this.getCommand("forcestart").setExecutor(new ForceStartCommand(this));
        this.getCommand("nightvision").setExecutor(new NightVisionCommand());
        this.getCommand("points").setExecutor(new PointsCommand(this));
        this.getCommand("stats").setExecutor(new StatsCommand(this));

        this.gameTracker = new GameTracker(this);
    }

    public GameTracker gameTracker() {
        return this.gameTracker;
    }

    public String gameWorldName() {
        return this.getDescription().getName();
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

    public Set<CaveOre> ores() {
        return this.ores;
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

    private HikariDataSource initDataSource() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }

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

package com.gamersafer.minecraft.abbacaving;

import com.gamersafer.minecraft.abbacaving.commands.*;
import com.gamersafer.minecraft.abbacaving.datasource.DataSource;
import com.gamersafer.minecraft.abbacaving.datasource.DummyDataSource;
import com.gamersafer.minecraft.abbacaving.datasource.SQLDataSource;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GameTracker;
import com.gamersafer.minecraft.abbacaving.guis.CosmeticGui;
import com.gamersafer.minecraft.abbacaving.listeners.*;
import com.gamersafer.minecraft.abbacaving.lobby.Lobby;
import com.gamersafer.minecraft.abbacaving.placeholders.GamePlaceholders;
import com.gamersafer.minecraft.abbacaving.player.PlayerCache;
import com.gamersafer.minecraft.abbacaving.tools.CosmeticRegistry;
import java.io.File;
import java.io.IOException;
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

    private final PlayerCache cache = new PlayerCache(this);
    private Set<CaveOre> ores;
    private Set<CaveLoot> loot;
    private GameTracker gameTracker;
    private Lobby lobby;
    private Map<String, List<Location>> mapSpawns;
    private DataSource dataSource;
    private FileConfiguration mapsConfig = new YamlConfiguration();
    private FileConfiguration messagesConfig = new YamlConfiguration();
    private FileConfiguration pointsConfig = new YamlConfiguration();
    private final Map<String, Game> maps = new HashMap<>();
    private CosmeticGui cosmeticGui;
    private CosmeticRegistry cosmeticRegistry;

    private InventoryListener inventoryListener;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.mapsConfig = this.fileConfiguration("maps.yml");
        this.messagesConfig = this.fileConfiguration("messages.yml");
        this.pointsConfig = this.fileConfiguration("points.yml");

        final File schematicDirectory = new File(this.getDataFolder(), "schematics");

        if (!schematicDirectory.exists()) {
            schematicDirectory.mkdirs();
        }

        this.loadData();

        this.inventoryListener = new InventoryListener(this);
        this.getServer().getPluginManager().registerEvents(this.inventoryListener, this);

        this.getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        this.getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerKillEntityListener(this), this);
        this.getServer().getPluginManager().registerEvents(new WorldListener(this), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new GamePlaceholders(this).register();
        }

        this.dataSource = this.getConfig().contains("local-testing") ? new DummyDataSource() : new SQLDataSource(this);
        this.dataSource.init();

        final ACLookupCommand acLookupCommand = new ACLookupCommand(this);
        this.getCommand("aclookup").setExecutor(acLookupCommand);
        this.getCommand("aclookup").setTabCompleter(acLookupCommand);

        this.getCommand("acreload").setExecutor(new ACReloadCommand(this));
        this.getCommand("bcastnp").setExecutor(new BroadcastNPCommand(this));
        this.getCommand("cosmetics").setExecutor(new CosmeticsCommand(this));

        final ForceStartCommand forceStartCommand = new ForceStartCommand(this);
        this.getCommand("forcestart").setExecutor(forceStartCommand);
        this.getCommand("forcestart").setTabCompleter(forceStartCommand);

        final JoinCommand joinCommand = new JoinCommand(this);
        this.getCommand("join").setExecutor(joinCommand);
        this.getCommand("join").setTabCompleter(joinCommand);

        this.getCommand("leave").setExecutor(new LeaveCommand(this));
        this.getCommand("nightvision").setExecutor(new NightVisionCommand(this));
        this.getCommand("points").setExecutor(new PointsCommand(this));
        this.getCommand("reset").setExecutor(new ResetCommand(this));

        final StatsCommand statsCommand = new StatsCommand(this);
        this.getCommand("stats").setExecutor(statsCommand);
        this.getCommand("stats").setTabCompleter(statsCommand);

        this.getCommand("respawns").setExecutor(new RespawnCountCommand(this));
        this.getCommand("debug").setExecutor(new DebugCommand(this));

        for (final String mapName : this.configuredMapNames()) {
            this.maps.put(mapName, new Game(this, mapName));
        }

        this.gameTracker = new GameTracker(this);
        this.lobby = new Lobby(this);
        this.cosmeticRegistry = new CosmeticRegistry(this);
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

    public DataSource playerDataSource() {
        return this.dataSource;
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

    public CosmeticGui cosmeticGui() {
        if (this.cosmeticGui == null) {
            this.cosmeticGui = new CosmeticGui(this); // Lazy init for items adder
        }
        return this.cosmeticGui;
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

    public Set<CaveOre> ores() {
        return this.ores;
    }

    private void loadData() {
        this.ores = new HashSet<>();

        for (final Map<?, ?> entry : this.pointsConfig.getMapList("ores")) {
            final Map<?, ?> value = (Map<?, ?>) entry.get("value");
            try {
                this.ores.add(new CaveOre(
                        (String) entry.get("name"),
                        (Integer) value.get("exact"),
                        (Integer) value.get("min"),
                        (Integer) value.get("max"),
                        (Double) value.get("probability"),
                        Material.valueOf((String) entry.get("block")),
                        new ItemStack(Material.valueOf((String) entry.get("drop")))
                ));
            } catch (IllegalArgumentException exception) {
                this.getLogger().warning("Failed to load cave ore from type: " + entry.get("drop"));
            }
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

    public CosmeticRegistry cosmeticRegistry() {
        return this.cosmeticRegistry;
    }

    public PlayerCache getPlayerCache() {
        return cache;
    }
}

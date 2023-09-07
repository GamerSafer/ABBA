package com.gamersafer.minecraft.abbacaving;

import com.gamersafer.minecraft.abbacaving.commands.*;
import com.gamersafer.minecraft.abbacaving.datasource.DataSource;
import com.gamersafer.minecraft.abbacaving.datasource.DummyDataSource;
import com.gamersafer.minecraft.abbacaving.datasource.SQLDataSource;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import com.gamersafer.minecraft.abbacaving.game.GameTracker;
import com.gamersafer.minecraft.abbacaving.game.map.MapPool;
import com.gamersafer.minecraft.abbacaving.guis.CosmeticGui;
import com.gamersafer.minecraft.abbacaving.listeners.*;
import com.gamersafer.minecraft.abbacaving.lobby.Lobby;
import com.gamersafer.minecraft.abbacaving.loot.LootHandler;
import com.gamersafer.minecraft.abbacaving.placeholders.GamePlaceholders;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.player.PlayerCache;
import com.gamersafer.minecraft.abbacaving.tools.CosmeticRegistry;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class AbbaCavingPlugin extends JavaPlugin {

    private final PlayerCache cache = new PlayerCache(this);
    private Lobby lobby;
    private DataSource dataSource;
    private FileConfiguration messagesConfig = new YamlConfiguration();
    private CosmeticGui cosmeticGui;
    private CosmeticRegistry cosmeticRegistry;
    private GameTracker gameTracker;
    private LootHandler lootHandler;
    private MapPool mapPool;

    private InventoryListener inventoryListener;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.messagesConfig = this.fileConfiguration("messages.yml");

        final File schematicDirectory = new File(this.getDataFolder(), "schematics");

        if (!schematicDirectory.exists()) {
            schematicDirectory.mkdirs();
        }

        this.inventoryListener = new InventoryListener(this);
        this.getServer().getPluginManager().registerEvents(this.inventoryListener, this);

        this.getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        this.getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerKillEntityListener(this), this);

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
        this.getCommand("delete-data").setExecutor(new DeleteDataCommand(this));
        this.gameTracker = new GameTracker();
        this.mapPool = new MapPool(this.getLogger(), this.fileConfiguration("maps.yml"), this);
        this.lobby = new Lobby(this);
        this.cosmeticRegistry = new CosmeticRegistry(this);
        this.lootHandler = new LootHandler(this.getLogger(), this.fileConfiguration("points.yml"));
    }

    @Override
    public void onDisable() {
        super.onDisable();

        for (GamePlayer player : new ArrayList<>(this.getPlayerCache().values())) {
            this.getPlayerCache().unloadAndComplete(player.playerUUID(), (stats) -> {
                player.data().saveAll();
            });
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.messagesConfig = this.fileConfiguration("messages.yml");
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

    public LootHandler getLootHandler() {
        return lootHandler;
    }

    public Lobby lobby() {
        return this.lobby;
    }

    public MapPool getMapPool() {
        return mapPool;
    }

    public String configMessage(final String name) {
        return this.messagesConfig.getString(name);
    }

    public CosmeticRegistry cosmeticRegistry() {
        return this.cosmeticRegistry;
    }

    public PlayerCache getPlayerCache() {
        return cache;
    }
}

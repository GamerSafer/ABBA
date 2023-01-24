package com.gamersafer.minecraft.abbacaving.game;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.validators.BlockValidator;
import com.gamersafer.minecraft.abbacaving.game.validators.YLevelValidator;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import com.gamersafer.minecraft.abbacaving.lobby.QueueState;
import com.gamersafer.minecraft.abbacaving.util.ItemBuilder;
import com.gamersafer.minecraft.abbacaving.util.Util;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.themoep.randomteleport.RandomTeleport;
import de.themoep.randomteleport.searcher.RandomSearcher;
import de.themoep.randomteleport.searcher.options.NotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.intellij.lang.annotations.Subst;

public class Game {

    private final AbbaCavingPlugin plugin;
    private final World world;
    private String gameId;
    private final String mapName;
    private final Map<String, GamePlayer> players = new HashMap<>();
    private Map<GamePlayer, Integer> leaderboard = new LinkedHashMap<>();
    private Map<UUID, Location> randomSpawns = new HashMap<>();
    private int counter;
    private boolean gracePeriod;
    private GameState state;
    private EditSession editSession = null;
    private final YLevelValidator yLevelValidator;
    private final BlockValidator blockValidator;

    public Game(final AbbaCavingPlugin plugin, final String mapName) {
        this.plugin = plugin;
        this.mapName = mapName;
        this.world = this.loadMap();

        this.yLevelValidator = new YLevelValidator(this);
        this.blockValidator = new BlockValidator(this);

        this.gameState(GameState.READY);
        this.counter(0);

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::nextTick, 0, 20);
    }

    public <T> T mapSetting(final String key) {
        final Object value = this.plugin.mapSettings(this.mapName).get(key);

        if (value != null) {
            return (T) value;
        }

        return (T) this.plugin.mapSettings("default-settings").get(key);
    }

    private void broadcastActionBar(final Component component) {
        for (final GamePlayer gp : this.players.values()) {
            gp.player().sendActionBar(component);
        }
    }

    private void playSound(final Sound sound) {
        for (final GamePlayer gp : this.players.values()) {
            gp.player().playSound(sound);
        }
    }

    public void broadcast(final String format, final Map<String, Component> placeholders) {
        final List<TagResolver> resolvers = new ArrayList<>();

        for (final Map.Entry<String, Component> entry : placeholders.entrySet()) {
            resolvers.add(TagResolver.resolver(entry.getKey(), Tag.inserting(entry.getValue())));
        }

        this.broadcast(format, resolvers.toArray(new TagResolver[0]));
    }

    public void broadcast(final String format) {
        this.broadcast(format, Map.of());
    }

    public void broadcast(final String format, final TagResolver... resolvers) {
        final Component message = MiniMessage.miniMessage().deserialize(format, resolvers);

        for (final GamePlayer gp : this.players.values()) {
            gp.player().sendMessage(message);
        }
    }

    public CompletableFuture<Location> randomLocation(final Player player) {
        final Plugin randomTPPlugin = Bukkit.getPluginManager().getPlugin("RandomTeleport");

        if (randomTPPlugin instanceof RandomTeleport randomTeleport) {
            final int minRadius = this.mapSetting("random-teleport.min-radius");
            final int maxRadius = this.mapSetting("random-teleport.max-radius");
            final int minY = this.mapSetting("random-teleport.min-y");
            final int maxY = this.mapSetting("random-teleport.max-y");

            final RandomSearcher randomSearcher = randomTeleport.getRandomSearcher(player, this.world().getSpawnLocation(),
                    minRadius, maxRadius, this.blockValidator);

            this.plugin.getLogger().info("Random searcher old max tries: [" + randomSearcher.getMaxTries() +
                    "]. New: [" + randomSearcher.getMaxTries() * 2 + "]");

            randomSearcher.setMaxTries(randomSearcher.getMaxTries() * 2);
            randomSearcher.setMinY(minY);
            randomSearcher.setMaxY(maxY);

            return randomSearcher.search();
        }

        return CompletableFuture.completedFuture(this.world().getSpawnLocation());
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

    public void gameState(final GameState state) {
        this.state = state;
    }

    public Collection<GamePlayer> players() {
        return this.players.values();
    }

    public String gameId() {
        return this.gameId;
    }

    public String mapName() {
        return this.mapName;
    }

    public void preparePlayerSpawn(final Player player) {
        // If the countdown cancels and restarts, don't find locations for players a second time
        if (this.randomSpawns.containsKey(player.getUniqueId())) {
            return;
        }

        this.randomLocation(player).whenComplete((location, exception) -> {
            if (exception != null) {
                if (exception instanceof NotFoundException notFoundException) {
                    this.plugin.getLogger().info("Could not find random spawn for player [" + player.getName() + "]");
                    this.plugin.getLogger().info("Reason: [" + notFoundException.getWhat() + "]. Teleporting player to map spawn.");
                } else {
                    exception.printStackTrace();
                }

                location = this.world.getSpawnLocation();
            }

            if (location != null) {
                this.plugin.getLogger().info("Player RTP location: " + location);

                if (this.state == GameState.STARTING || this.state == GameState.RUNNING) {
                    player.teleport(location);
                } else {
                    this.randomSpawns.put(player.getUniqueId(), location);
                }
            }
        });
    }

    public void addPlayer(final GamePlayer gamePlayer) {
        if (this.players.put(gamePlayer.player().getName(), gamePlayer) == null) {
            this.broadcast(this.plugin.configMessage("player-joined"), Map.of("player", gamePlayer.player().displayName()));

            this.preparePlayer(gamePlayer.player());

            final Location randomSpawn = this.randomSpawns.get(gamePlayer.player().getUniqueId());

            if (randomSpawn == null) {
                this.plugin.getLogger().info("Could not find random spawn for player [" + gamePlayer.player().getName() + "]");
                //gamePlayer.player().teleport(this.world().getSpawnLocation());
            } else {
                gamePlayer.player().teleport(randomSpawn);
            }

            // Remove random spawns when used, players should have a unique experience each round
            this.randomSpawns.remove(gamePlayer.player().getUniqueId());
        } else {
            this.plugin.getLogger().info("Player [" + gamePlayer.player().getName() + "] already in game [" + this.mapName + "]");
        }
    }

    public void increasePlayerScore(final GamePlayer gamePlayer, final int amount) {
        final int currentScore = this.leaderboard.computeIfAbsent(gamePlayer, x -> 0);

        this.leaderboard.put(gamePlayer, currentScore + amount);
        this.updateLeaderboard();
    }

    public GamePlayer removePlayer(final Player player, final boolean quit) {
        final GamePlayer gp = this.players.get(player.getName());
        if (gp == null) return null;

        this.players.remove(player.getName());
        this.leaderboard.remove(gp);
        this.updateLeaderboard();
        this.plugin.lobby().resetPlayer(player);

        if (quit) {
            this.broadcast(this.plugin.configMessage("player-left"), Map.of("player", player.displayName()));
            this.plugin.message(player, this.plugin.configMessage("leave-game"), Map.of("map", this.mapName()));
        }

        return gp;
    }

    public void saveHotbar(final GamePlayer player) {
        final Map<Integer, String> hotbarSlots = new HashMap<>();

        for (int i = 0; i <= 8; i++) {
            final ItemStack slotItem = player.player().getInventory().getItem(i);

            if (slotItem == null || slotItem.getType() == Material.AIR) {
                continue;
            }

            hotbarSlots.put(i, slotItem.getType().toString());
        }

        player.hotbarLayout(hotbarSlots);
        this.plugin.playerDataSource().savePlayerHotbar(player);
    }

    public GamePlayer player(final Player player) {
        for (final GamePlayer gp : this.players.values()) {
            if (gp.player().getUniqueId().equals(player.getUniqueId())) {
                return gp;
            }
        }
        return null;
    }

    private void counter(final int counter) {
        this.counter = counter;
    }

    public int maxPlayersPerRound() {
        return this.mapSetting("maximum-players-per-round");
    }

    private void nextTick() {
        if (this.state == GameState.READY) {
            return;
        }

        if (this.state == GameState.RUNNING) {
            final int gameDurationSeconds = this.mapSetting("duration-seconds");
            final int gracePeriodSeconds = this.mapSetting("grace-period-duration-seconds");

            if (this.counter > 0) {
                if (this.gracePeriod) {
                    final int gracePeriodRemaining = this.counter - (gameDurationSeconds - gracePeriodSeconds);

                    this.broadcastActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("game-grace-period"),
                            TagResolver.resolver("seconds", Tag.inserting(Component.text(gracePeriodRemaining))),
                            TagResolver.resolver("optional-s", Tag.inserting(Component.text(gracePeriodRemaining != 1 ? "s" : "")))));
                } else {
                    this.broadcastActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("game-ending-ab"),
                            TagResolver.resolver("counter", Tag.inserting(Component.text(this.counter)))));
                }
            } else if (this.counter == 0) {
                this.broadcastActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("game-over")));
            }

            if (this.counter == gameDurationSeconds - gracePeriodSeconds) {
                this.gracePeriod = false;
                this.broadcast(this.plugin.configMessage("grace-period-ended"));
            } else if ((this.counter % 60 == 0 || this.counter == 30 || this.counter == 15
                    || this.counter == 10 || this.counter <= 5) && this.counter != 0) {
                final int minutes = this.counter / 60;
                if (minutes > 0 && minutes <= 5) {
                    this.broadcast(this.plugin.configMessage("game-ending"), Map.of(
                            "amount", Component.text(minutes),
                            "time-type", Component.text(minutes == 1 ? "minute" : "minutes")
                    ));

                    this.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.NEUTRAL, 1f, 1f));
                } else if (this.counter <= 30) {
                    this.broadcast(this.plugin.configMessage("game-ending"), Map.of(
                            "amount", Component.text(this.counter),
                            "time-type", Component.text(this.counter == 1 ? "second" : "seconds")
                    ));

                    this.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.NEUTRAL, 1f, 1f));
                }
            } else if (this.counter == 0) {
                this.stop();
            }
        }

        this.counter--;
    }

    public void start(final String gameId) {
        this.gameId = gameId;
        this.gracePeriod = true;
        this.leaderboard.clear();

        final int gracePeriodSeconds = this.mapSetting("duration-seconds");
        this.counter(gracePeriodSeconds);
        this.broadcast(this.plugin.configMessage("game-started"));

        for (final GamePlayer gp : this.players.values()) {
            final Location loc = this.world.getSpawnLocation();

            gp.spawnLocation(loc);
            this.leaderboard.put(gp, 0);
            this.preparePlayer(gp.player());
            gp.score(0);
            gp.bucketUses(0);
            this.startingInventory(gp);
            this.setupGUIs(gp);
            gp.player().setGameMode(GameMode.SURVIVAL);
        }

        this.updateLeaderboard();

        this.broadcast(this.plugin.configMessage("game-objective"));
        this.playSound(Sound.sound(Key.key("block.note_block.snare"), Sound.Source.NEUTRAL, 1f, 1f));
        this.gameState(GameState.RUNNING);
    }

    private void stop() {
        this.plugin.getLogger().info("Game ended for map [" + this.mapName + "] and game ID [" + this.gameId + "]");

        this.broadcast(this.plugin.configMessage("game-ended"));

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

            this.broadcast(this.plugin.configMessage("game-win"), Map.of(
                    "player", winner.getKey().player().displayName(),
                    "score", Component.text(Util.addCommas(winner.getValue())),
                    "optional-s", Component.text(winner.getKey().score() != 1 ? "s" : "")
            ));

            this.broadcast(this.plugin.configMessage("game-top-players"));

            final List<GamePlayer> sorted = new ArrayList<>(sortedLeaderboards.keySet());

            for (int i = 0; i < Math.min(sorted.size(), 5); i++) {
                final GamePlayer gp = sorted.get(i);
                this.broadcast(this.plugin.configMessage("top-player"),
                        TagResolver.resolver("index", Tag.inserting(Component.text(i + 1))),
                        TagResolver.resolver("score", Tag.inserting(Component.text(Util.addCommas(this.leaderboard.get(gp))))),
                        TagResolver.resolver("displayname", Tag.inserting(gp.player().displayName())));
            }
        } else {
            this.broadcast(this.plugin.configMessage("game-end-draw"));
        }

        final ConfigurationSection sound = this.mapSetting("end-of-round.sound");

        if (sound != null) {
            final @Subst("minecraft:block.bell.use") String soundKey = sound.getString("sound");
            final String sourceKey = sound.getString("source");

            try {
                if (soundKey != null && sourceKey != null) {
                    final Key key = Key.key(soundKey);
                    final Sound.Source source = Sound.Source.valueOf(sourceKey.toUpperCase());
                    final double pitch = sound.getDouble("pitch");
                    final double volume = sound.getDouble("volume");

                    final Sound mainSound = Sound.sound(key, source, (float) pitch, (float) volume);

                    for (final GamePlayer gp : this.players.values()) {
                        gp.player().playSound(mainSound);
                    }
                }
            } catch (final InvalidKeyException exception) {
                this.plugin.getLogger().warning("Invalid key for game.end-of-round.sound.key");
            }
        }

        final String titleText = this.mapSetting("end-of-round.title");
        final String subtitleText = this.mapSetting("end-of-round.subtitle");

        Component title = Component.empty();
        Component subtitle = Component.empty();

        if (titleText != null) {
            title = MiniMessage.miniMessage().deserialize(titleText);
        }

        if (subtitleText != null) {
            subtitle = MiniMessage.miniMessage().deserialize(subtitleText);
        }

        final Title mainTitle = Title.title(title, subtitle);

        for (final GamePlayer gp : this.players.values()) {
            this.preparePlayer(gp.player());
            gp.player().showTitle(mainTitle);
        }

        final List<String> endOfRoundCommands = this.mapSetting("end-of-round.commands");

        if (endOfRoundCommands != null) {
            for (final String command : endOfRoundCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }

        for (final Entity entity : this.world().getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }

        this.gameState(GameState.DONE);
        this.counter(0);

        final String schematicFile = this.mapSetting("end-of-round.schematic.name");

        if (schematicFile != null && !schematicFile.isBlank()) {
            final File schematicDirectory = new File(this.plugin.getDataFolder(), "schematics");
            final File schematic = new File(schematicDirectory, schematicFile);

            if (schematic.exists()) {
                this.plugin.getLogger().info("Pasting end of round schematic");

                final ClipboardFormat format = ClipboardFormats.findByFile(schematic);

                try (final ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
                    final Clipboard clipboard = reader.read();

                    try {
                        this.editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(this.world));

                        final Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(this.editSession)
                                .to(BlockVector3.at(
                                        this.mapSetting("end-of-round.schematic.x"),
                                        this.mapSetting("end-of-round.schematic.y"),
                                        this.mapSetting("end-of-round.schematic.z")
                                ))
                                .ignoreAirBlocks(false)
                                .build();

                        Operations.complete(operation);
                        // Replace with this.editSession.flushQueue() if there are issues with undoing pastes after closing this
                        this.editSession.close();
                    } catch (final WorldEditException e) {
                        e.printStackTrace();
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }

        final ConfigurationSection tpSection = this.mapSetting("end-of-round.teleport");

        if (tpSection != null) {
            final Location teleportLocation = new Location(this.world(), tpSection.getDouble("x"), tpSection.getDouble("y"),
                    tpSection.getDouble("z"));

            for (final GamePlayer gamePlayer : this.players.values()) {
                gamePlayer.player().teleport(teleportLocation);
            }
        }

        final int postGameGracePeriod = this.mapSetting("game-end-grace-period-seconds");

        this.broadcast(this.plugin.configMessage("game-end-lobby"), Map.of("counter", Component.text(postGameGracePeriod)));

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            for (final GamePlayer gp : this.players.values()) {
                gp.score(0);
                this.sendToLobby(gp.player());
            }

            this.resetMap();
            this.leaderboard.clear();
            this.players.clear();
            this.gameState(GameState.READY);

            final LobbyQueue queue = this.plugin.lobby().lobbyQueue(this.mapName);

            if (queue != null) {
                queue.state(QueueState.WAITING);
            }
        }, 20L * postGameGracePeriod);
    }

    private CoreProtectAPI coreProtect() {
        final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (!(plugin instanceof CoreProtect coreProtect)) {
            return null;
        }

        // Check that the API is enabled
        if (!coreProtect.isEnabled() || !coreProtect.getAPI().isEnabled()) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (coreProtect.getAPI().APIVersion() < 9) {
            return null;
        }

        return coreProtect.getAPI();
    }

    private World loadMap() {
        this.plugin.getLogger().info("Loading map '" + this.mapName + "'...");

        final WorldCreator worldCreator = new WorldCreator(this.mapName).keepSpawnLoaded(TriState.FALSE);

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

    private void resetMap() {
        final CoreProtectAPI coreProtect = this.coreProtect();

        final int gameDurationSeconds = this.mapSetting("duration-seconds");
        final int offsetSeconds = 300; // 5 minutes, this makes sure the restore covers the entire game duration

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.plugin.getLogger().info("Rolling back block changes (CoreProtect)");

            coreProtect.performRollback(gameDurationSeconds + offsetSeconds, null, null, null,
                    null, null, 10000, this.world().getSpawnLocation());

            this.plugin.getLogger().info("Block changes rolled back");
        });

        if (this.editSession != null) {
            this.plugin.getLogger().info("Rolling back schematics");

            this.editSession.undo(this.editSession);

            this.plugin.getLogger().info("Schematics rolled back");
        }

        int removed = 0;
        int items = 0;

        for (final Entity e : this.world.getEntities()) {
            if (e.getType() == EntityType.PLAYER) {
                continue;
            }

            if (e.getType() == EntityType.DROPPED_ITEM) {
                items++;
            }

            removed++;
            e.remove();
        }

        this.plugin.getLogger().info("Removed " + removed + " entities (" + items + " were items)");
    }

    private final ItemStack PICKAXE = new ItemBuilder(Material.DIAMOND_PICKAXE)
            .unbreakable(true)
            .enchantment(Enchantment.SILK_TOUCH, 1)
            .miniMessageName("<green><bold>Starter Pickaxe")
            .build();

    private final ItemStack SWORD = new ItemBuilder(Material.IRON_SWORD)
            .miniMessageName("<green><bold>Starter Sword")
            .build();

    private final ItemStack BOW = new ItemBuilder(Material.BOW)
            .unbreakable(true)
            .enchantment(Enchantment.ARROW_INFINITE, 1)
            .miniMessageName("<green><bold>Infinite Bow")
            .build();

    private final ItemStack SHOVEL = new ItemBuilder(Material.IRON_SHOVEL)
            .miniMessageName("<green><bold>Starter Shovel")
            .build();

    private final ItemStack BEEF = new ItemBuilder(Material.COOKED_BEEF)
            .miniMessageName("<green><bold>Infinite Steak Supply")
            .build();

    private final ItemStack COBBLESTONE = new ItemBuilder(Material.COBBLESTONE)
            .miniMessageName("<green><bold>Infinite Cobble")
            .build();

    private final ItemStack BUCKET = new ItemStack(Material.WATER_BUCKET);

    private final ItemStack TORCH = new ItemBuilder(Material.TORCH)
            .miniMessageName("<green><bold>Infinite Torch")
            .build();

    private final ItemStack ARROW = new ItemStack(Material.ARROW);

    private final Map<String, ItemStack> hotbarItemCache = Map.of(
            "DIAMOND_PICKAXE", this.PICKAXE,
            "IRON_SWORD", this.SWORD,
            "BOW", this.BOW,
            "IRON_SHOVEL", this.SHOVEL,
            "COOKED_BEEF", this.BEEF,
            "COBBLESTONE", this.COBBLESTONE,
            "WATER_BUCKET", this.BUCKET,
            "TORCH", this.TORCH,
            "ARROW", this.ARROW
    );

    public void startingInventory(final GamePlayer player) {
        final Inventory inv = player.player().getInventory();

        final ItemStack shield = new ItemStack(Material.SHIELD);
        final Damageable damageable = (Damageable) shield.getItemMeta();
        damageable.setDamage(168);
        shield.setItemMeta(damageable);
        player.player().getInventory().setItemInOffHand(shield);

        player.player().getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.player().getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.player().getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.player().getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

        // Load existing hotbar layout
        if (!player.hotbarLayout().isEmpty()) {
            for (final Map.Entry<Integer, String> entry : player.hotbarLayout().entrySet()) {
                player.player().getInventory().setItem(entry.getKey(), this.hotbarItemCache.get(entry.getValue()));
            }

            return;
        }

        inv.addItem(this.PICKAXE, this.SWORD, this.BOW, this.SHOVEL,
                this.BEEF, this.COBBLESTONE, this.BUCKET, this.TORCH, this.ARROW);
    }

    public void preparePlayer(final Player player) {
        final int maxHealth = this.mapSetting("player-health");
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setLevel(0);
        player.setExp(0);
        player.getInventory().clear();
    }

    public void updateLeaderboard() {
        this.leaderboard = this.sortByValue(this.leaderboard, true);
    }

    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
            final Map<K, V> map, final boolean desc) {
        final List<Entry<K, V>> entries = new LinkedList<>(map.entrySet());

        entries.sort((o1, o2) -> {
            if (desc) {
                return o2.getValue().compareTo(o1.getValue());
            } else {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        final Map<K, V> result = new LinkedHashMap<>(entries.size());

        for (final Entry<K, V> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public void sendToLobby(final Player player) {
        player.teleport(new Location(
                Bukkit.getWorld(this.plugin.getConfig().getString("lobby-spawn-location.world")),
                this.plugin.getConfig().getDouble("lobby-spawn-location.x"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.y"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.z"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.yaw"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.pitch")));
    }

    final static ItemStack BACKGROUND_ITEM = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
    final static ItemStack CHEST = new ItemBuilder(Material.CHEST).name(Component.text("Save Hotbar Layout")).build();
    final static ItemStack BEACON = new ItemBuilder(Material.BEACON).name(Component.text("Cosmetics")).build();
    final static ItemStack STATS_ARROW = new ItemBuilder(Material.ARROW).name(Component.text("Statistics")).build();
    final static ItemStack REDSTONE = new ItemBuilder(Material.REDSTONE_BLOCK).name(Component.text("Return to Lobby")).build();

    private void setupGUIs(final GamePlayer player) {
        // Inventory slots 9 (top-left) to 35 (bottom-right) are the player's inventory grid
        for (int index = 9; index <= 35; index++) {
            player.player().getInventory().setItem(index, BACKGROUND_ITEM);
        }

        // Populate GUI buttons
        player.player().getInventory().setItem(19, CHEST);
        player.player().getInventory().setItem(21, BEACON);
        player.player().getInventory().setItem(23, STATS_ARROW);
        player.player().getInventory().setItem(25, REDSTONE);
    }

}

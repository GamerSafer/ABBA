package com.gamersafer.minecraft.abbacaving.game;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.map.GameMap;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.player.GameStats;
import com.gamersafer.minecraft.abbacaving.tools.ToolManager;
import com.gamersafer.minecraft.abbacaving.util.*;
import com.google.common.collect.Collections2;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.intellij.lang.annotations.Subst;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Game {

    private String gameId = GameToken.randomToken();

    private final AbbaCavingPlugin plugin;
    private final GameMap map;
    private final List<GamePlayer> players = new ArrayList<>();
    private Map<GamePlayer, Integer> leaderboard = new LinkedHashMap<>();
    private Map<UUID, Location> spawnLocations = new HashMap<>();
    private final Map<GamePlayer, GameStats> gameData = new HashMap<>();
    private int counter;
    private boolean gracePeriod;
    private GameState state = GameState.RUNNING;
    private final BukkitTask task;

    private final ForwardingAudience globalAudience = () -> Collections2.transform(players, GamePlayer::player);

    public Game(final AbbaCavingPlugin plugin, final GameMap map) {
        this.plugin = plugin;
        this.map = map;
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0, 20);
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
        return this.players;
    }

    public String gameId() {
        return this.gameId;
    }

    public GameMap getMap() {
        return map;
    }

    public GameStats getGameData(GamePlayer player) {
        return this.gameData.get(player);
    }

    public void registerPlayer(final GamePlayer gamePlayer) {
        if (this.players.add(gamePlayer)) {
            gamePlayer.game(this);
            this.gameData.put(gamePlayer, new GameStats(gamePlayer, this.plugin, this));
            this.leaderboard.put(gamePlayer, 0);
            this.plugin.gameTracker().registerPlayerGame(gamePlayer.playerUUID(), this);
        } else {
            this.plugin.getLogger().info("Player [" + gamePlayer.player().getName() + "] already in game [" + this.map.getName() + "]");
        }
    }

    public void unregisterPlayer(final GamePlayer gamePlayer) {
        this.players.remove(gamePlayer);
        gamePlayer.game(null);
        this.plugin.gameTracker().unregisterPlayerGame(gamePlayer.playerUUID(), this);
        this.leaderboard.remove(gamePlayer);
        this.updateLeaderboard();
    }

    public void respawn(final GamePlayer gamePlayer) {
        this.intoWorld(gamePlayer);

        gamePlayer.data().negateRespawn();
        GameStats stats = this.getGameData(gamePlayer);
        stats.isDead(false);
        stats.hasRespawned(true);

        Messages.messageComponents(this.globalAudience, this.plugin.configMessage("player-respawned"), Map.of("player", gamePlayer.player().displayName()));
    }

    public void playerChosenDisconnect(Player player) {
        final GamePlayer gp = this.plugin.getPlayerCache().getLoaded(player);
        GameStats stats = this.getGameData(gp);

        Messages.messageComponents(this.globalAudience, this.plugin.configMessage("player-left"), Map.of(
                "player", player.displayName(),
                "score", Component.text(stats.score())
        ));

        Messages.message(player, this.plugin.configMessage("leave-game"), Map.of(
                "map", this.map.getName(),
                "score", Integer.toString(stats.score())
        ));

        this.playerExit(player);
    }

    void playerExit(Player player) {
        final GamePlayer gp = this.plugin.getPlayerCache().getLoaded(player);

        this.unregisterPlayer(gp);

        this.plugin.lobby().sendToLobby(player);
    }

    public void intoWorld(final GamePlayer gamePlayer) {
        gamePlayer.player().teleport(this.spawnLocations.get(gamePlayer.playerUUID()));
        this.preparePlayer(gamePlayer.player());
        this.setupGUIs(gamePlayer);
        this.startingInventory(gamePlayer);
    }

    public void increasePlayerScore(final GamePlayer gamePlayer, final int amount) {
        final int currentScore = this.leaderboard.computeIfAbsent(gamePlayer, x -> 0);

        this.leaderboard.put(gamePlayer, currentScore + amount);
        this.updateLeaderboard();
    }

    public int maxPlayersPerRound() {
        return this.map.mapSetting("maximum-players-per-round");
    }

    public boolean countMobKills() {
        return this.map.mapSetting("count-points-per-mob-kill");
    }

    private void tick() {
        if (this.players.isEmpty()) {
            // There are 0 players left in the round (everyone quit/died), end the round early
            if (this.map.mapSetting("end-empty-games")) {
                this.stop();
                return;
            }
        }
        // TEMPORARY
        for (GamePlayer player : new ArrayList<>(this.players)) {
            if (player.player() == null) {
                this.plugin.getLogger().warning("IMPROPER PLAYER CLEANUP DETECTED: " + player.playerUUID());
                this.plugin.getLogger().warning("PLAYER DATA DUMP: " + player.data());
                this.plugin.getLogger().warning("QUEUE: " + player.queue());
                this.plugin.getLogger().warning("GAME: " + player.game() + " THIS: " + this);
                this.unregisterPlayer(player);
            }
        }

        for (GamePlayer gamePlayer : this.players) {
            Block targetBlock = gamePlayer.player().getTargetBlock(5);
            if (targetBlock != null && targetBlock.getType().name().toLowerCase().contains("deepslate")) {
                gamePlayer.player().addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 20, 2, true, false, false));
            }
        }

        final int gameDurationSeconds = this.map.mapSetting("duration-seconds");
        final int gracePeriodSeconds = this.map.mapSetting("grace-period-duration-seconds");

        if (this.counter > 0) {
            if (this.gracePeriod) {
                final int gracePeriodRemaining = this.counter - (gameDurationSeconds - gracePeriodSeconds);

                this.globalAudience.sendActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("game-grace-period"),
                        TagResolver.resolver("seconds", Tag.inserting(Component.text(gracePeriodRemaining))),
                        TagResolver.resolver("optional-s", Tag.inserting(Component.text(gracePeriodRemaining != 1 ? "s" : "")))));
            } else {
                this.globalAudience.sendActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("game-ending-ab"),
                        TagResolver.resolver("counter", Tag.inserting(Component.text(this.counter)))));
            }
        } else if (this.counter == 0) {
            this.globalAudience.sendActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("game-over")));
        }

        if (this.counter == gameDurationSeconds - gracePeriodSeconds) {
            this.gracePeriod = false;
            Messages.message(this.globalAudience, this.plugin.configMessage("grace-period-ended"));

            Sounds.goatHorn(this.globalAudience);
        } else if ((this.counter % 60 == 0 || this.counter == 30 || this.counter == 15
                || this.counter == 10 || this.counter <= 5) && this.counter != 0) {
            final int minutes = this.counter / 60;
            if (minutes > 0 && minutes <= 5) {
                Messages.messageComponents(this.globalAudience, this.plugin.configMessage("game-ending"), Map.of(
                        "amount", Component.text(minutes),
                        "time-type", Component.text(minutes == 1 ? "minute" : "minutes")
                ));

                Sounds.pling(this.globalAudience);
            } else if (this.counter <= 30) {
                Messages.messageComponents(this.globalAudience, this.plugin.configMessage("game-ending"), Map.of(
                        "amount", Component.text(this.counter),
                        "time-type", Component.text(this.counter == 1 ? "second" : "seconds")
                ));

                Sounds.pling(this.globalAudience);
            }
        } else if (this.counter == 0) {
            this.stop();
        }

        this.counter--;
    }

    public void start() {
        this.gracePeriod = (int) this.map.mapSetting("grace-period-duration-seconds") >= 2;

        this.counter = this.map.mapSetting("duration-seconds");
        Messages.message(this.globalAudience, this.plugin.configMessage("game-started"));

        for (final GamePlayer gp : this.players) {
            this.intoWorld(gp);
            Messages.messageComponents(gp.player(), this.plugin.configMessage("player-joined"), Map.of("player", gp.player().displayName()));
        }

        this.updateLeaderboard();

        Messages.message(this.globalAudience, this.plugin.configMessage("game-objective"));
        this.globalAudience.playSound(Sound.sound(Key.key("block.note_block.snare"), Sound.Source.NEUTRAL, 1f, 1f));
    }

    public void stop() {
        this.plugin.getLogger().info("Game ended for map [" + this.map.getName() + "] and game ID [" + this.gameId + "]");
        // Remove dead players from leaderboard
        for (GamePlayer player : new ArrayList<>(this.players)) {
            if (this.getGameData(player).isDead()) {
                this.playerExit(player.player());
            }
        }

        this.plugin.playerDataSource().saveFinishedGame(this);
        Messages.message(this.globalAudience, this.plugin.configMessage("game-ended"));

        if (this.leaderboard.size() == 0) {
            Messages.message(this.globalAudience, this.plugin.configMessage("game-end-draw"));
        } else {
            final Map<GamePlayer, Integer> sortedLeaderboards = this.leaderboard.entrySet().stream()
                    .sorted(Map.Entry.<GamePlayer, Integer>comparingByValue().reversed())
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (x, y) -> {
                        throw new AssertionError();
                    }, LinkedHashMap::new));

            final Entry<GamePlayer, Integer> winner = sortedLeaderboards.entrySet().iterator().next();
            winner.getKey().data().incrementWins();
            GameStats stats = this.getGameData(winner.getKey());

            Messages.messageComponents(this.globalAudience, this.plugin.configMessage("game-win"), Map.of(
                    "player", winner.getKey().player().displayName(),
                    "score", Component.text(Util.addCommas(winner.getValue())),
                    "optional-s", Component.text(stats.score() != 1 ? "s" : "")
            ));

            Messages.message(this.globalAudience, this.plugin.configMessage("game-top-players"));

            final List<GamePlayer> sorted = new ArrayList<>(sortedLeaderboards.keySet());

            // Give rewards
            for (int i = 0; i < sorted.size(); i++) {
                String value = this.map.mapSetting("end-of-round.rewards." + (i + 1));
                if (value == null) {
                    value = this.map.mapSetting("end-of-round.rewards.else");
                }

                if (value != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.replaceAll("<player>", sorted.get(i).player().getName()));
                }
            }

            for (int i = 0; i < Math.min(sorted.size(), 5); i++) {
                final GamePlayer gp = sorted.get(i);
                Messages.message(this.globalAudience, this.plugin.configMessage("top-player"),
                        TagResolver.resolver("index", Tag.inserting(Component.text(i + 1))),
                        TagResolver.resolver("score", Tag.inserting(Component.text(Util.addCommas(this.leaderboard.get(gp))))),
                        TagResolver.resolver("displayname", Tag.inserting(gp.player().displayName())));
            }
        }

        final ConfigurationSection sound = this.map.mapSetting("end-of-round.sound");

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

                    this.globalAudience.playSound(mainSound);
                }
            } catch (final InvalidKeyException exception) {
                this.plugin.getLogger().warning("Invalid key for game.end-of-round.sound.key");
            }
        }

        final String titleText = this.map.mapSetting("end-of-round.title");
        final String subtitleText = this.map.mapSetting("end-of-round.subtitle");

        Component title = Component.empty();
        Component subtitle = Component.empty();

        if (titleText != null) {
            title = MiniMessage.miniMessage().deserialize(titleText);
        }

        if (subtitleText != null) {
            subtitle = MiniMessage.miniMessage().deserialize(subtitleText);
        }

        final Title mainTitle = Title.title(title, subtitle);

        this.globalAudience.showTitle(mainTitle);

        final List<String> endOfRoundCommands = this.map.mapSetting("end-of-round.commands");

        if (endOfRoundCommands != null) {
            for (final String command : endOfRoundCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }

        this.gameState(GameState.DONE);

        final ConfigurationSection tpSection = this.map.mapSetting("end-of-round.teleport");
        if (tpSection != null) {
            final Location teleportLocation = new Location(this.map.getWorld(), tpSection.getDouble("x"), tpSection.getDouble("y"), tpSection.getDouble("z"));

            for (final GamePlayer gamePlayer : this.players) {
                gamePlayer.player().teleport(teleportLocation);
                gamePlayer.player().setGameMode(GameMode.SURVIVAL);
            }
        }

        for (final GamePlayer gp : this.players) {
            Stats.dumpGameStats(gp.player(), this, this.getGameData(gp));
        }

        final int postGameGracePeriod = this.map.mapSetting("game-end-grace-period-seconds");

        Messages.messageComponents(this.globalAudience, this.plugin.configMessage("game-end-lobby"), Map.of("counter", Component.text(postGameGracePeriod)));

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            for (final GamePlayer gp : new ArrayList<>(this.players)) {
                Game.this.playerExit(gp.player());
            }
            new BukkitRunnable(){

                @Override
                public void run() {
                    plugin.getMapPool().releaseGame(Game.this);
                }
            }.runTaskLater(this.plugin, 100);
        }, 20L * postGameGracePeriod);
        this.task.cancel();
    }

    public void startingInventory(final GamePlayer player) {
        ToolManager.apply(player);
    }

    public void preparePlayer(final Player player) {
        final int maxHealth = this.map.mapSetting("player-health");
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setLevel(0);
        player.setExp(0);
        player.getInventory().clear();
        player.setInvisible(false);
        player.setGameMode(GameMode.SURVIVAL);
    }

    public void updateLeaderboard() {
        this.leaderboard = this.sortByValue(this.leaderboard, true);
    }

    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map, final boolean desc) {
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

    final static ItemStack BACKGROUND_ITEM = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
    final static ItemStack CHEST = new ItemBuilder(Material.CHEST)
            .name(Components.plainText("Save Hotbar Layout"))
            .lore(List.of(Components.plainText("Shift+Right Click to Reset")
                    .color(NamedTextColor.GRAY))
            )
            .build();
    final static ItemStack BEACON = new ItemBuilder(Material.BEACON)
            .name(Components.plainText("Cosmetics"))
            .build();
    final static ItemStack STATS_ARROW = new ItemBuilder(Material.ARROW)
            .name(Components.plainText("Statistics"))
            .build();
    final static ItemStack REDSTONE = new ItemBuilder(Material.REDSTONE_BLOCK)
            .name(Components.plainText("Return to Lobby"))
            .build();

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

    public ForwardingAudience getGlobalAudience() {
        return globalAudience;
    }

    public Map<UUID, Location> getSpawnLocations() {
        return spawnLocations;
    }
}

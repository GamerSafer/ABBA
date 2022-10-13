package com.gamersafer.minecraft.abbacaving.lobby;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.util.Util;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Lobby implements Listener {

    private final AbbaCavingPlugin plugin;
    private List<UUID> playerLobbyQueue = new LinkedList<>();
    private LobbyState lobbyState = LobbyState.WAITING;
    private int counter = 0;

    public Lobby(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::nextTick, 0, 20);
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public int counter() {
        return this.counter;
    }

    public void counter(final int counter) {
        this.counter = counter;
    }

    public LobbyState lobbyState() {
        return this.lobbyState;
    }

    public void lobbyState(final LobbyState lobbyState) {
        this.lobbyState = lobbyState;
    }

    public List<UUID> nextGamePlayerQueue() {
        final int maxPlayers = this.plugin.mapSettings("default-settings").getInt("maximum-players-per-round");
        final int playersToGrab = Math.min(maxPlayers, this.playerLobbyQueue.size());

        return this.playerLobbyQueue.subList(0, playersToGrab);
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        event.joinMessage(null);

        event.getPlayer().setGameMode(GameMode.ADVENTURE);
        event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        event.getPlayer().setHealth(20);
        event.getPlayer().setFoodLevel(20);

        event.getPlayer().getInventory().clear();
        event.getPlayer().getInventory().setArmorContents(null);

        for (final Game game : this.plugin.gameTracker().currentGames()) {
            if (game.acceptingNewPlayers()) {
                this.plugin.gameTracker().addPlayerToGame(game, event.getPlayer());
                event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("joining-in-progress")));
                return;
            }
        }

        event.getPlayer().teleport(new Location(
                Bukkit.getWorld(this.plugin.getConfig().getString("lobby-spawn-location.world")),
                this.plugin.getConfig().getDouble("lobby-spawn-location.x"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.y"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.z"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.yaw"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.pitch")));

        this.playerLobbyQueue.add(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.playerLobbyQueue.remove(event.getPlayer().getUniqueId());
        // TODO: stop counter when lobby player counts < minimum required players
    }

    private void nextTick() {
        if (this.lobbyState == LobbyState.WAITING) {
            if (this.playerLobbyQueue.size() >= this.playersRequiredToStart()) {
                //                if (caveGenerator != null && !caveGenerator.isReady()) {
                //                    plugin.getLogger().info("Waiting for the world to be generated...");
                //                } else {
                //                    preStart();
                //                }
                this.preStart();
            } else {
                for (final UUID uuid : this.nextGamePlayerQueue()) {
                    final Player player = Bukkit.getPlayer(uuid);

                    if (player != null) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("lobby-count"),
                                TagResolver.resolver("current", Tag.inserting(Component.text(this.playerLobbyQueue.size()))),
                                TagResolver.resolver("max", Tag.inserting(Component.text(this.playersRequiredToStart())))));
                    }
                }
            }
        } else if (this.lobbyState == LobbyState.STARTING) {
            if (this.playerLobbyQueue.size() < this.playersRequiredToStart()) {
                this.cancelPreStart();
                return;
            }

            if (this.counter >= 0) {
                for (final UUID uuid : this.nextGamePlayerQueue()) {
                    final Player player = Bukkit.getPlayer(uuid);

                    if (player != null) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("game-starting"),
                                TagResolver.resolver("seconds", Tag.inserting(Component.text(this.counter))),
                                TagResolver.resolver("optional-s", Tag.inserting(Component.text(this.counter != 1 ? "s" : "")))));
                    }
                }
            }

            if (this.counter == 0) {
                // TODO: better map picking, don't start two games on one map
                final List<String> mapNames = this.plugin.configuredMapNames();
                final String mapName = mapNames.get(ThreadLocalRandom.current().nextInt(mapNames.size()));

                this.start(mapName);
            } else {
                if (this.counter % 60 == 0 || this.counter == 30 || this.counter == 15
                        || this.counter == 10 || this.counter <= 5) {
                    for (final UUID uuid : this.nextGamePlayerQueue()) {
                        final Player player = Bukkit.getPlayer(uuid);

                        if (player != null) {
                            this.plugin.message(player, this.plugin.configMessage("game-starting"),
                                    TagResolver.resolver("seconds", Tag.inserting(Component.text(this.counter))),
                                    TagResolver.resolver("optional-s", Tag.inserting(Component.text(this.counter != 1 ? "s" : ""))));
                        }
                    }
                }
            }
        }

        if (this.lobbyState == LobbyState.STARTING) {
            this.counter--;
        }
    }

    public void preStart() {
        this.counter(this.plugin.mapSettings("default-settings").getInt("start-countdown-seconds"));
        this.lobbyState(LobbyState.STARTING);
    }

    public void cancelPreStart() {
        this.lobbyState(LobbyState.WAITING);
        this.counter(0);
        // TODO: Feedback to let players know the countdown was cancelled. Message? Actionbar? Sound?
    }

    public Game start(final String mapName) {
        this.lobbyState = LobbyState.WAITING;
        this.counter = 0; // TODO: replace this and other lines with method invocation | this.counter(0);

        for (final UUID playerId : this.playerLobbyQueue) {
            final Player player = Bukkit.getPlayer(playerId);

            if (player != null) {
                this.plugin.message(player, this.plugin.configMessage("preparing-map"));
            }
        }

        final Game game = this.plugin.game(mapName);

        this.plugin.gameTracker().currentGames().add(game);
        final List<UUID> uuidsToRemove = new ArrayList<>();

        for (final UUID playerId : this.playerLobbyQueue) {
            if (game.players().size() >= game.maxPlayersPerRound()) {
                break;
            }

            final Player player = Bukkit.getPlayer(playerId);

            if (player != null) {
                this.plugin.gameTracker().addPlayerToGame(game, player);
                uuidsToRemove.add(player.getUniqueId());
            }
        }

        this.playerLobbyQueue.removeAll(uuidsToRemove);
        game.start(Util.randomString(6));

        return game;
    }

    public void stop(final Game game) {
        for (final GamePlayer player : game.players()) {
            this.playerLobbyQueue.add(player.player().getUniqueId());
        }
    }

    public int playersRequiredToStart() {
        return this.plugin.mapSettings("default-settings").getInt("players-required-to-start");
    }

}

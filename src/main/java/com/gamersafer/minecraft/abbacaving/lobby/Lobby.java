package com.gamersafer.minecraft.abbacaving.lobby;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final Map<String, LobbyQueue> lobbyQueues = new HashMap<>();

    public Lobby(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::nextTick, 0, 20);
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        for (final String mapName : this.plugin.configuredMapNames()) {
            this.lobbyQueues.put(mapName, new LobbyQueue(mapName, new LinkedList<>()));
        }
    }

    public LobbyQueue lobbyQueue(final String mapName) {
        return this.lobbyQueues.get(mapName);
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
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        for (final LobbyQueue queue : this.lobbyQueues.values()) {
            queue.playerQueue().remove(event.getPlayer().getUniqueId());
        }
    }

    private void nextTick() {
        for (final LobbyQueue queue : this.lobbyQueues.values()) {
            if (queue.state() == QueueState.WAITING) {
                this.handleQueueWaiting(queue);
            } else if (queue.state() == QueueState.STARTING) {
                this.handleQueueStarting(queue);
                queue.decrementCounter();
            }
        }
    }
    
    private void handleQueueWaiting(final LobbyQueue queue) {
        if (queue.playerQueue().size() >= this.playersRequiredToStart()) {
            this.preStart(queue);
        } else {
            for (final UUID uuid : queue.playerQueue()) {
                final Player player = Bukkit.getPlayer(uuid);

                if (player != null) {
                    player.sendActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("lobby-count"),
                            TagResolver.resolver("current", Tag.inserting(Component.text(queue.playerQueue().size()))),
                            TagResolver.resolver("max", Tag.inserting(Component.text(this.playersRequiredToStart())))));
                }
            }
        }
    }
    
    private void handleQueueStarting(final LobbyQueue queue) {
        if (queue.playerQueue().size() < this.playersRequiredToStart()) {
            this.cancelPreStart(queue);
            return;
        }

        if (queue.counter() >= 0) {
            for (final UUID uuid : queue.playerQueue()) {
                final Player player = Bukkit.getPlayer(uuid);

                if (player != null) {
                    player.sendActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("game-starting"),
                            TagResolver.resolver("seconds", Tag.inserting(Component.text(queue.counter()))),
                            TagResolver.resolver("optional-s", Tag.inserting(Component.text(queue.counter() != 1 ? "s" : "")))));
                }
            }
        }

        if (queue.counter() == 0) {
            this.start(queue);
        } else {
            if (queue.counter() % 60 == 0 || queue.counter() == 30 || queue.counter() == 15
                    || queue.counter() == 10 || queue.counter() <= 5) {
                for (final UUID uuid : queue.playerQueue()) {
                    final Player player = Bukkit.getPlayer(uuid);

                    if (player != null) {
                        this.plugin.message(player, this.plugin.configMessage("game-starting"),
                                TagResolver.resolver("seconds", Tag.inserting(Component.text(queue.counter()))),
                                TagResolver.resolver("optional-s", Tag.inserting(Component.text(queue.counter() != 1 ? "s" : ""))));
                    }
                }
            }
        }
    }

    public void preStart(final LobbyQueue queue) {
        queue.counter(this.plugin.mapSettings("default-settings").getInt("start-countdown-seconds"));
        queue.state(QueueState.STARTING);
    }

    public void cancelPreStart(final LobbyQueue queue) {
        queue.state(QueueState.WAITING);
        queue.counter(0);
        // TODO: Feedback to let players know the countdown was cancelled. Message? Actionbar? Sound?
    }

    public Game start(final LobbyQueue queue) {
        queue.state(QueueState.LOCKED);
        queue.counter(0);

        for (final UUID playerId : queue.playerQueue()) {
            final Player player = Bukkit.getPlayer(playerId);

            if (player != null) {
                this.plugin.message(player, this.plugin.configMessage("preparing-map"));
            }
        }

        final Game game = this.plugin.game(queue.mapName());

        this.plugin.gameTracker().currentGames().add(game);
        final List<UUID> uuidsToRemove = new ArrayList<>();

        for (final UUID playerId : queue.playerQueue()) {
            if (game.players().size() >= game.maxPlayersPerRound()) {
                break;
            }

            final Player player = Bukkit.getPlayer(playerId);

            if (player != null) {
                this.plugin.gameTracker().addPlayerToGame(game, player);
                uuidsToRemove.add(player.getUniqueId());
            }
        }

        queue.playerQueue().removeAll(uuidsToRemove);
        game.start(Util.randomString(6));

        return game;
    }

    public int playersRequiredToStart() {
        return this.plugin.mapSettings("default-settings").getInt("players-required-to-start");
    }

}

package com.gamersafer.minecraft.abbacaving.lobby;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.map.GameMap;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.util.Messages;
import com.gamersafer.minecraft.abbacaving.util.Sounds;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LobbyQueue {

    private final AbbaCavingPlugin plugin;
    private final GameMap map;
    private final List<UUID> playerQueue = new ArrayList<>();

    private int counter = 0;
    private QueueState state;
    private boolean forceStart = false;
    private final Map<UUID, CompletableFuture<?>> waitingFutures = new HashMap<>();
    private final Map<UUID, Location> spawnLocations = new HashMap<>();
    private final ForwardingAudience queueAudience = () -> Collections2.transform(this.playerQueue, (Function<UUID, Audience>) Bukkit::getPlayer);

    public LobbyQueue(AbbaCavingPlugin plugin, final GameMap gameMap) {
        this.plugin = plugin;
        this.map = gameMap;
        this.state = QueueState.WAITING;
    }

    public boolean acceptingNewPlayers() {
        // Dont let player join in last 5 seconds
        if (this.state == QueueState.STARTING && this.counter <= 5) {
            return false;
        }

        return this.playerQueue.size() < this.map.maxPlayers();
    }

    void addPlayer(final UUID uuid) {
        this.playerQueue.add(uuid);
    }

    void removePlayer(final UUID uuid) {
        this.playerQueue.remove(uuid);
        this.spawnLocations.remove(uuid);
        CompletableFuture<?> future = this.waitingFutures.remove(uuid);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void addWaitingFuture(UUID uuid, final CompletableFuture<?> future) {
        this.waitingFutures.put(uuid, future);
    }

    public List<UUID> getPlayerQueue() {
        return playerQueue;
    }

    public GameMap getMap() {
        return map;
    }

    public ForwardingAudience getQueueAudience() {
        return queueAudience;
    }


    public void setState(QueueState state) {
        this.state = state;
    }

    public QueueState getState() {
        return state;
    }

    public void tick() {
        if (this.state == QueueState.WAITING) {
            this.handleQueueWaiting();
        } else if (this.state == QueueState.STARTING) {
            this.handleQueueStarting();
            this.counter--;
        }
    }

    private void handleQueueWaiting() {
        if (this.queueSize() >= this.map.getPlayersRequiredToStart() || this.forceStart) {
            this.counter = this.map.getStartTime();
            this.state = QueueState.STARTING;
        } else {
            this.queueAudience.sendActionBar(
                    MiniMessage.miniMessage().deserialize(this.plugin.configMessage("lobby-count"),
                            TagResolver.resolver("current", Tag.inserting(Component.text(this.queueSize()))),
                            TagResolver.resolver("max", Tag.inserting(Component.text(this.map.getPlayersRequiredToStart()))))
            );
        }
    }

    private void handleQueueStarting() {
        if (!this.forceStart && this.playerQueue.size() < this.map.getPlayersRequiredToStart()) {
            this.state = QueueState.WAITING;
            this.counter = 0;

            this.queueAudience.sendActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("queue-stopped")));
            return;
        }

        if (this.counter >= 0) {
            this.queueAudience.sendActionBar(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("game-starting"),
                    TagResolver.resolver("seconds", Tag.inserting(Component.text(this.counter))),
                    TagResolver.resolver("optional-s", Tag.inserting(Component.text(this.counter != 1 ? "s" : "")))));
        }


        if (this.counter <= 0) {
            if (this.isWaitingForFutures()) {
                this.plugin.getLogger().log(Level.INFO, "Waiting for tasks to complete...");
            } else {
                Game game = this.plugin.getMapPool().startGame(this.map);
                this.waitingFutures.clear();
                this.state = QueueState.LOCKED;
                this.counter = 0;

                if (game == null) {
                    this.queueAudience.sendMessage(Component.text("Oops.. looks like an issue occured here. Please let the server owners know."));
                } else {
                    game.getSpawnLocations().putAll(this.spawnLocations);
                    this.spawnLocations.clear();
                    for (final UUID playerId : this.playerQueue) {
                        GamePlayer player = this.plugin.getPlayerCache().getLoaded(playerId);
                        game.registerPlayer(player);
                        player.queue(null);
                    }

                    this.playerQueue.clear();
                    Messages.message(this.queueAudience, this.plugin.configMessage("preparing-map"));
                    game.start();
                }

            }
        } else if (this.counter % 60 == 0 || this.counter == 30 || this.counter == 15 || this.counter == 10 || this.counter <= 5) {
                for (GamePlayer player : this.plugin.getPlayerCache().values()) {
                    CommandSender sender = player.player();
                    // If player in game, or game state is not finished
                    Game game = this.plugin.gameTracker().findGame(player);
                    if (game == null) {
                        Messages.message(sender, this.plugin.configMessage("game-starting"),
                                TagResolver.resolver("map", Tag.inserting(Component.text(this.getMap().getName()))),
                                TagResolver.resolver("seconds", Tag.inserting(Component.text(this.counter))),
                                TagResolver.resolver("optional-s", Tag.inserting(Component.text(this.counter != 1 ? "s" : ""))));
                        Sounds.pling(sender);
                    }
                }
        }
    }

    public int getCounter() {
        return this.counter;
    }

    public void forceStart() {
        this.forceStart = true;
    }

    public void addSpawnLocation(UUID uuid, Location location) {
        this.spawnLocations.put(uuid, location);
    }

    private int queueSize() {
        return this.playerQueue.size();
    }

    private boolean isWaitingForFutures() {
        for (final CompletableFuture<?> future : this.waitingFutures.values()) {
            if (!future.isDone()) {
                return true;
            }
        }

        return false;
    }
}

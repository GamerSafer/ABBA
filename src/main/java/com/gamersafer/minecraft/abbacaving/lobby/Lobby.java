package com.gamersafer.minecraft.abbacaving.lobby;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Lobby implements Listener {

    private final AbbaCavingPlugin plugin;
    private final List<UUID> lobbyPlayers = new ArrayList<>();

    private LobbyState lobbyState = LobbyState.WAITING;
    private int counter = 0;

    public Lobby(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::nextTick, 0, 20);
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

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        event.joinMessage(null);

        event.getPlayer().setGameMode(GameMode.ADVENTURE);
        event.getPlayer().teleport(new Location(
                Bukkit.getWorld(this.plugin.getConfig().getString("lobby-spawn-location.world")),
                this.plugin.getConfig().getDouble("lobby-spawn-location.x"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.y"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.z"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.yaw"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.pitch")));

        this.lobbyPlayers.add(event.getPlayer().getUniqueId());

        // TODO: join in-progress games instead of lobby if there's room in a game?
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.lobbyPlayers.remove(event.getPlayer().getUniqueId());
        // TODO: stop counter when lobby player counts < minimum required players
    }

    private void nextTick() {
        if (this.lobbyState == LobbyState.WAITING) {
            if (this.lobbyPlayers.size() >= this.plugin.getConfig().getInt("game.players-required-to-start")) {
                //                if (caveGenerator != null && !caveGenerator.isReady()) {
                //                    plugin.getLogger().info("Waiting for the world to be generated...");
                //                } else {
                //                    preStart();
                //                }
                this.preStart();
            }
        } else if (this.lobbyState == LobbyState.STARTING) {
            if (this.counter >= 0) {
                Bukkit.getServer().sendActionBar(MiniMessage.miniMessage().deserialize("<gray>Starting In: <green>" + this.counter));
            }

            if (this.counter == 0) {
                this.start();
            } else {
                if (this.counter % 60 == 0 || this.counter == 30 || this.counter == 15
                        || this.counter == 10 || this.counter <= 5) {
                    this.plugin.broadcast(this.plugin.configMessage("game-starting"), Map.of(
                            "seconds", Component.text(this.counter),
                            "optional-s", Component.text(this.counter != 1 ? "s" : "")
                    ));
                }
            }
        }

        this.counter++;
    }

    public void preStart() {
        this.counter(this.plugin.getConfig().getInt("game.start-countdown-seconds"));
        this.lobbyState(LobbyState.STARTING);
    }

    public void start() {
        final Game game = new Game(this.plugin, this.plugin.mapSpawns(), Util.randomString(6));

        this.plugin.gameTracker().currentGames().add(game);

        for (final UUID playerId : this.lobbyPlayers) {
            final Player player = Bukkit.getPlayer(playerId);

            if (player != null) {
                this.plugin.gameTracker().addPlayerToGame(game, player);
            }
        }

        this.lobbyPlayers.clear();
        game.start();

        // TODO: Handle cases where more players join the lobby than the game
    }

}

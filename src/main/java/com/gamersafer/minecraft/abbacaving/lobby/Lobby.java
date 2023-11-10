package com.gamersafer.minecraft.abbacaving.lobby;

import be.maximvdw.featherboard.api.FeatherBoardAPI;
import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.map.GameMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Lobby implements Listener {

    private final AbbaCavingPlugin plugin;
    private final Map<GameMap, LobbyQueue> lobbyQueues = new LinkedHashMap<>();

    public Lobby(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0, 20);
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        for (final GameMap map : this.plugin.getMapPool().getMaps()) {
            this.lobbyQueues.put(map, new LobbyQueue(plugin, map));
        }
    }

    @Nullable
    public LobbyQueue pickFirstQueue() {
        Collection<LobbyQueue> queues = lobbyQueues.values();
        LobbyQueue firstEmpty = null;
        for (LobbyQueue queue : queues) {
            if (firstEmpty == null && queue.getState() != QueueState.LOCKED && queue.acceptingNewPlayers()) {
                firstEmpty = queue;
            }

            // Pick any queues open that are not empty
            if (queue.getState() != QueueState.LOCKED && queue.acceptingNewPlayers() && !queue.getPlayerQueue().isEmpty()) {
                return queue;
            }
        }

        return firstEmpty;
    }

    public List<LobbyQueue> activeQueues() {
        final List<LobbyQueue> queues = new ArrayList<>();

        for (final LobbyQueue queue : this.lobbyQueues.values()) {
            if (queue.getState() == QueueState.WAITING || queue.getState() == QueueState.STARTING) {
                queues.add(queue);
            }
        }

        return queues;
    }

    @Nullable
    public LobbyQueue lobbyQueue(final GameMap mapName) {
        return this.lobbyQueues.get(mapName);
    }

    private void tick() {
        for (final LobbyQueue queue : this.lobbyQueues.values()) {
            queue.tick();
        }
    }

    public void join(final LobbyQueue queue, final Player player) {
        this.plugin.getPlayerCache().getLoaded(player).queue(queue);
        queue.addPlayer(player.getUniqueId());
        queue.addWaitingFuture(player.getUniqueId(), queue.getMap().randomLocation(player).thenAccept((loc) -> {
            queue.addSpawnLocation(player.getUniqueId(), loc);
        }));
    }

    public void leave(final LobbyQueue queue, final Player player) {
        this.plugin.getPlayerCache().getLoaded(player).queue(null);
        queue.removePlayer(player.getUniqueId());
    }

    public void sendToLobby(Player player) {
        player.teleport(Lobby.this.lobbySpawn());

        this.plugin.lobby().resetPlayer(player);
    }

    public void resetPlayer(final Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);
        player.setFoodLevel(20);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setInvisible(false);

        FeatherBoardAPI.showScoreboard(player, "spawn");
    }

    private Location lobbySpawn() {
        return new Location(
                Bukkit.getWorld(this.plugin.getConfig().getString("lobby-spawn-location.world")),
                this.plugin.getConfig().getDouble("lobby-spawn-location.x"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.y"),
                this.plugin.getConfig().getDouble("lobby-spawn-location.z"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.yaw"),
                (float) this.plugin.getConfig().getDouble("lobby-spawn-location.pitch"));
    }

}

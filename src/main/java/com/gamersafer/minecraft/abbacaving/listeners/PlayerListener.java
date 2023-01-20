package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.util.Util;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Collection;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public PlayerListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final GamePlayer gp = this.plugin.gameTracker().gamePlayer(event.getPlayer());
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.playerDataSource().loadPlayerStats(gp));
    }

    @EventHandler
    public void onPlayerChat(final AsyncChatEvent event) {
        final GamePlayer gamePlayer = this.plugin.gameTracker().findPlayerInGame(event.getPlayer());

        // Lobby players should only speak to other lobby players
        if (gamePlayer == null || this.plugin.lobby().playerInLobby(event.getPlayer())) {
            event.viewers().removeIf(viewer -> {
                if (viewer instanceof Player recipient) {
                    return !this.plugin.lobby().playerInLobby(recipient);
                }

                return false;
            });
        } else {
            // Players in games should only talk to other players in the same game
            event.viewers().removeIf(viewer -> {
                if (viewer instanceof Player recipient) {
                    final Game recipientGame = this.plugin.gameTracker().findGame(recipient);
                    final Game senderGame = this.plugin.gameTracker().findGame(event.getPlayer());

                    return !(recipientGame != null && recipientGame.equals(senderGame));
                }

                return false;
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        event.quitMessage(null);

        final GamePlayer gp = this.plugin.gameTracker().removePlayer(event.getPlayer(), true);

        if (gp != null) {
            this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.playerDataSource().savePlayerStats(gp));
        }
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final GamePlayer gamePlayer = this.plugin.gameTracker().findPlayerInGame(player);
        final Game game = this.plugin.gameTracker().findGame(player);

        if (gamePlayer == null || game == null) {
            return;
        }

        if (!player.hasPermission("abbacaving.respawn") || gamePlayer.hasRespawned()) {
            game.broadcast(this.plugin.configMessage("player-died"), Map.of("player", player.displayName(),
                    "score", Component.text(gamePlayer.score())));

            final Collection<GamePlayer> players = game.players();

            if (players.size() > 1) {
                game.broadcast(this.plugin.configMessage("remaining-players"), Map.of(
                        "count", Component.text(players.size()),
                        "optional-s", Component.text(players.size() != 1 ? "s" : "")
                ));
            }

            gamePlayer.isDead(true);
            game.removePlayer(player, false);
            game.sendToLobby(player);

            return;
        }

        gamePlayer.hasRespawned(true);
        gamePlayer.score(0);
        gamePlayer.bucketUses(0);
        game.updateLeaderboard();
        game.startingInventory(player);

        // TODO: RTP player in world when respawning

        game.broadcast(this.plugin.configMessage("player-respawned"), Map.of("player", player.displayName()));
    }

    @EventHandler
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        if (!event.getPlayer().hasPermission("abbacaving.inventory")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        this.handleEntityEvent(event.getEntity(), event);
    }

    private void handleEntityEvent(final Entity target, final Cancellable cancellable) {
        if (target instanceof Player player) {
            final GamePlayer gp = this.plugin.gameTracker().findPlayerInGame(player);

            if (gp != null && gp.isDead()) {
                cancellable.setCancelled(true);
                return;
            }
        }

        final Game game = this.plugin.gameTracker().findGame(target.getWorld());

        if (game == null || game.isGracePeriod() || game.gameState() == GameState.DONE) {
            cancellable.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(final EntityTargetEvent event) {
        if (event.getTarget() != null) {
            this.handleEntityEvent(event.getTarget(), event);
        }
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        this.handleEntityEvent(event.getEntity(), event);
    }

    @EventHandler
    public void onPlayerBucketFill(final PlayerBucketFillEvent event) {
        if (event.getItemStack().getType() == Material.LAVA_BUCKET) {
            event.setCancelled(true);
            this.plugin.message(event.getPlayer(), "<gray>You cannot fill your bucket with <red>Lava<gray>.");
        }
    }

    @EventHandler
    public void onPlayerEat(final PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.COOKED_BEEF) {
            final int slot = event.getPlayer().getInventory().first(Material.COOKED_BEEF);
            event.getPlayer().getInventory().setItem(slot,
                    Util.displayName(new ItemStack(Material.COOKED_BEEF), "<green><gold>Infinite Steak Supply"));
        }
    }

    @EventHandler
    public void onPlayerWorldChange(final PlayerChangedWorldEvent event) {
        final Game game = this.plugin.gameTracker().findGame(event.getPlayer());

        if (game == null || game.gameState() == GameState.DONE) {
            return;
        }

        if (!event.getPlayer().getWorld().equals(game.world())) {
            game.removePlayer(event.getPlayer(), true);
        }
    }

}

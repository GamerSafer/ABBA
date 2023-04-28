package com.gamersafer.minecraft.abbacaving.listeners;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.datasource.DataSource;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.util.ItemBuilder;
import com.gamersafer.minecraft.abbacaving.util.Util;
import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.google.common.collect.Iterables;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Collection;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final AbbaCavingPlugin plugin;
    private final ChestGui gui;

    public PlayerListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;

        final Component title = MiniMessage.miniMessage().deserialize(this.plugin.configMessage("respawn-title"));
        this.gui = new ChestGui(3, ComponentHolder.of(title));

        final StaticPane backgroundPane = new StaticPane(0, 0, 9, this.gui.getRows());
        backgroundPane.fillWith(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        backgroundPane.setPriority(Pane.Priority.LOWEST);

        this.gui.addPane(backgroundPane);

        final StaticPane buttonPane = new StaticPane(0, 0, 9, this.gui.getRows());
        buttonPane.setPriority(Pane.Priority.HIGHEST);

        final ItemStack yesItem = new ItemBuilder(Material.EMERALD_BLOCK).name(Component.text("Yes, Continue!")).build();
        final GuiItem yesButton = new GuiItem(yesItem, onClick -> {
            final GamePlayer gamePlayer = this.plugin.gameTracker().gamePlayer(onClick.getWhoClicked().getUniqueId());
            final Game game = gamePlayer.gameStats().game();
            if (game.gameState() == GameState.RUNNING) {
                game.respawnPlayer(gamePlayer);
            }
        });
        buttonPane.addItem(yesButton, 2, 1);

        final ItemStack spectateItem = new ItemBuilder(Material.FEATHER).name(Component.text("Spectate")).build();
        final GuiItem spectateButton = new GuiItem(spectateItem, onClick -> {
            final Player clicker = (Player) onClick.getWhoClicked();
            if (!clicker.hasPermission("abbacaving.spectate")) {
                clicker.closeInventory();
                this.plugin.message(clicker, this.plugin.configMessage("no-permission"));
                return;
            }

            final GamePlayer gamePlayer = this.plugin.gameTracker().gamePlayer(onClick.getWhoClicked().getUniqueId());
            final Game game = gamePlayer.gameStats().game();
            final Player player = Iterables.getFirst(game.players(), null).player();

            clicker.teleport(player);
            clicker.setGameMode(GameMode.SPECTATOR);
        });
        buttonPane.addItem(spectateButton, 4, 1);

        final ItemStack noItem = new ItemBuilder(Material.REDSTONE_BLOCK).name(Component.text("No, return to lobby.")).build();
        final GuiItem noButton = new GuiItem(noItem, onClick -> {
            onClick.getWhoClicked().closeInventory();
            this.plugin.lobby().sendToLobby(onClick.getWhoClicked());
        });
        buttonPane.addItem(noButton, 6, 1);

        this.gui.addPane(buttonPane);
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

        final GamePlayer gp = this.plugin.gameTracker().gamePlayer(event.getPlayer());

        if (gp == null) {
            this.plugin.getLogger().info("Could not save stats for [" + event.getPlayer().getName() + "]");
            return;
        }

        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final DataSource dataSource = this.plugin.playerDataSource();
            dataSource.savePlayerStats(gp);
            dataSource.savePlayerRespawns(gp);
        });

        final GamePlayer.GameStats gameStats = gp.gameStats();
        if (gameStats == null) {
            return;
        }

        final Game game = gameStats.game();
        if (game == null || game.gameState() == GameState.DONE) {
            return;
        }

        game.removePlayer(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final GamePlayer gamePlayer = this.plugin.gameTracker().gamePlayer(player);

        if (gamePlayer == null || gamePlayer.gameStats() == null) {
            this.plugin.getLogger().info("Skipping game respawn for " + player.getName() + " due to missing gameStats");
            return;
        }

        if (gamePlayer.gameStats().showRespawnGui()) {
            event.setRespawnLocation(gamePlayer.gameStats().respawnLocation());
        }
    }

    @EventHandler
    public void onPlayerSpawn(final PlayerPostRespawnEvent event) {
        final Player player = event.getPlayer();
        final GamePlayer gamePlayer = this.plugin.gameTracker().gamePlayer(player);

        if (gamePlayer == null || gamePlayer.gameStats() == null) {
            this.plugin.getLogger().info("Skipping respawn-gui for " + player.getName() + " due to missing gameStats");
            return;
        }

        if (gamePlayer.gameStats().showRespawnGui()) {
            this.gui.show(player);
            gamePlayer.gameStats().showRespawnGui(false);
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

        final boolean hasPermission = player.hasPermission("abbacaving.respawn");
        final boolean hasRespawned = gamePlayer.gameStats().hasRespawned();

        this.plugin.getLogger().info(player.getName() + " has died in a match [hasPermission=" + hasPermission + ", hasRespawned=" + hasRespawned + "]");

        game.broadcast(this.plugin.configMessage("player-died"), Map.of("player", player.displayName(),
                "score", Component.text(gamePlayer.gameStats().score())));

        final Collection<GamePlayer> players = game.players();

        if (players.size() - 1 >= 1) {
            game.broadcast(this.plugin.configMessage("remaining-players"), Map.of(
                    "count", Component.text(players.size() - 1),
                    "optional-s", Component.text(players.size() != 1 ? "s" : "")
            ));
        }

        if (hasPermission && !hasRespawned && gamePlayer.respawns() > 0) {
            gamePlayer.gameStats().respawnLocation(event.getPlayer().getLocation());
            gamePlayer.gameStats().hasRespawned(true);
            gamePlayer.gameStats().showRespawnGui(true);
            gamePlayer.gameStats().score(0);
            game.updateLeaderboard();
            game.startingInventory(gamePlayer);
            gamePlayer.negateRespawn();

            return;
        }

        gamePlayer.gameStats().isDead(true);
        game.removePlayer(player, false);
        this.plugin.lobby().sendToLobby(player);
    }

    @EventHandler
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        if (!event.getPlayer().hasPermission("abbacaving.build")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        this.handleEntityEvent(event.getEntity(), event);
    }

    private void handleEntityEvent(final Entity target, final Cancellable cancellable) {
        final Game game = this.plugin.gameTracker().findGame(target.getWorld());

        if (game == null || game.isGracePeriod() || game.gameState() == GameState.DONE) {
            cancellable.setCancelled(true);
            return;
        }

        if (target instanceof Player player) {
            final GamePlayer gp = game.player(player);

            if (gp != null && gp.gameStats() != null && gp.gameStats().isDead()) {
                cancellable.setCancelled(true);
            }
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

        if (!event.getPlayer().getWorld().equals(game.world()) && !game.player(event.getPlayer()).gameStats().isDead()) {
            game.removePlayer(event.getPlayer(), true);
        }
    }

}

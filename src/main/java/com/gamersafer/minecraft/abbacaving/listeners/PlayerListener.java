package com.gamersafer.minecraft.abbacaving.listeners;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.player.GameStats;
import com.gamersafer.minecraft.abbacaving.tools.ToolTypes;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import com.gamersafer.minecraft.abbacaving.util.ItemBuilder;
import com.gamersafer.minecraft.abbacaving.util.Messages;
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
import org.bukkit.event.player.*;
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
            Player player = (Player) onClick.getWhoClicked();
            Game game = this.plugin.gameTracker().findGame(player);
            GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player.getUniqueId());

            if (game.gameState() == GameState.RUNNING) {
                game.respawn(gamePlayer);
            }
        });
        buttonPane.addItem(yesButton, 2, 1);

        final ItemStack spectateItem = new ItemBuilder(Material.FEATHER).name(Component.text("Spectate")).build();
        final GuiItem spectateButton = new GuiItem(spectateItem, onClick -> {
            final Player clicker = (Player) onClick.getWhoClicked();
            if (!clicker.hasPermission("abbacaving.spectate")) {
                clicker.closeInventory();
                Messages.message(clicker, this.plugin.configMessage("no-permission"));
                return;
            }

            Player player = (Player) onClick.getWhoClicked();
            Game game = this.plugin.gameTracker().findGame(player);
            final Player teleportPlayer = Iterables.getFirst(game.players(), null).player();

            clicker.teleport(teleportPlayer);
            clicker.setGameMode(GameMode.SPECTATOR);
        });
        buttonPane.addItem(spectateButton, 4, 1);

        final ItemStack noItem = new ItemBuilder(Material.REDSTONE_BLOCK).name(Component.text("No, return to lobby.")).build();
        final GuiItem noButton = new GuiItem(noItem, onClick -> {
            Player player = (Player) onClick.getWhoClicked();
            player.closeInventory();

            Game game = this.plugin.gameTracker().findGame(player);
            if (game != null) {
                this.plugin.lobby().sendToLobby(player); // Will reset the player
            }
        });
        this.gui.setOnClose(event -> {
            Player player = (Player) event.getPlayer();
            Game game = this.plugin.gameTracker().findGame(player);
            GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player);

            // will be false if respawned, can be null if game is ended
            if (game != null && game.getGameData(gamePlayer).isDead()) {
                this.plugin.lobby().sendToLobby(player); // Will reset the player
            }
        });

        buttonPane.addItem(noButton, 6, 1);

        this.gui.addPane(buttonPane);
    }

    @EventHandler
    public void onPlayerJoin(final AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            this.plugin.getPlayerCache().preload(event.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerChat(final AsyncChatEvent event) {
        final Game game = this.plugin.gameTracker().findGame(event.getPlayer());

        // Lobby players should only speak to other lobby players
        if (game == null) {
            event.viewers().removeIf(viewer -> {
                if (viewer instanceof Player recipient) {
                    GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(recipient);
                    return gamePlayer.game() != null;
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
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getPlayerCache().joinPreloadedOrLoad(event.getPlayer().getUniqueId());
        this.plugin.lobby().sendToLobby(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        event.quitMessage(null);
        final GamePlayer gp = this.plugin.getPlayerCache().getLoaded(event.getPlayer());

        if (gp == null) {
            this.plugin.getLogger().info("Could not save stats for [" + event.getPlayer().getName() + "]");
            return;
        }

        Game game = gp.game();
        if (game != null) {
            game.playerChosenDisconnect(event.getPlayer());
        }

        LobbyQueue queue = gp.queue();
        if (queue != null) {
            this.plugin.lobby().leave(queue, event.getPlayer());
        }
        this.plugin.getPlayerCache().unloadAndComplete(gp.playerUUID(), (stats) -> {
            gp.data().saveAll();
        });
    }

    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.plugin.gameTracker().findGame(player);
        final GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player);

        if (game == null) {
            this.plugin.getLogger().info("Skipping respawn-gui for " + player.getName() + " due to missing game");
            return;
        }

        GameStats stats = game.getGameData(gamePlayer);

        if (stats.showRespawnGui()) {
            event.setRespawnLocation(game.getSpawnLocations().get(player.getUniqueId()));
        }
    }

    @EventHandler
    public void onPlayerSpawn(final PlayerPostRespawnEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.plugin.gameTracker().findGame(player);
        final GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player);

        if (game == null) {
            this.plugin.getLogger().info("Skipping respawn-gui for " + player.getName() + " due to missing game");
            return;
        }

        GameStats stats = game.getGameData(gamePlayer);
        if (stats.showRespawnGui()) {
            this.gui.show(player);
            gamePlayer.player().setInvisible(true);
            stats.showRespawnGui(false);
        }
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final Game game = this.plugin.gameTracker().findGame(player);
        final GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player);

        if (gamePlayer == null || game == null) {
            return;
        }

        final GameStats stats = game.getGameData(gamePlayer);

        Messages.messageComponents(
                game.getGlobalAudience(),
                this.plugin.configMessage("player-died"), Map.of("player", player.displayName(), "score", Component.text(stats.score()))
        );

        final Collection<GamePlayer> players = game.players();
        if (players.size() - 1 >= 1) {
            Messages.messageComponents(
                    game.getGlobalAudience(),
                    this.plugin.configMessage("remaining-players"), Map.of(
                            "count", Component.text(players.size() - 1),
                            "optional-s", Component.text(players.size() != 1 ? "s" : "")
                    )
            );
        }

        final boolean hasPermission = player.hasPermission("abbacaving.respawn");
        final boolean hasRespawned = stats.hasRespawned();

        if (hasPermission && !hasRespawned && gamePlayer.data().respawns() > 0) {
            stats.showRespawnGui(true);
        }

        stats.isDead(true);
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
        final Game game = this.plugin.gameTracker().getGame(target.getWorld());

        if (game == null || game.isGracePeriod() || game.gameState() == GameState.DONE) {
            cancellable.setCancelled(true);
            return;
        }

        if (target instanceof Player player) {
            final GamePlayer gp = this.plugin.getPlayerCache().getLoaded(player);
            final GameStats stats = game.getGameData(gp);

            if (stats.isDead()) {
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
            Messages.message(event.getPlayer(), "<gray>You cannot fill your bucket with <red>Lava<gray>.");
        }
    }

    @EventHandler
    public void onPlayerEat(final PlayerItemConsumeEvent event) {
        final ItemStack item = event.getItem();
        final SlottedHotbarTool toolType = SlottedHotbarTool.stored(item);
        if (toolType == ToolTypes.BEEF) {
            event.setReplacement(item);
        }
    }

    @EventHandler
    public void onPlayerWorldChange(final PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.plugin.gameTracker().findGame(player);

        if (game == null || game.gameState() == GameState.DONE) {
            return;
        }

        if (!event.getPlayer().getWorld().equals(game.getMap().getWorld())) {
            game.playerChosenDisconnect(event.getPlayer());
        }
    }

}

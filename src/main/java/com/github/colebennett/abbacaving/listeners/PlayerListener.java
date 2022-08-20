package com.github.colebennett.abbacaving.listeners;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.GamePlayer;
import com.github.colebennett.abbacaving.game.GameState;
import com.github.colebennett.abbacaving.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public PlayerListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(final PlayerLoginEvent event) {
        if (this.plugin.currentGame().gameState() == GameState.RUNNING || this.plugin.currentGame().gameState() == GameState.DONE) {
            event.setResult(Result.KICK_OTHER);
            event.kickMessage(MiniMessage.miniMessage().deserialize("<red>A game is currently in progress."));
            return;
        }

        if (this.plugin.currentGame().players().size() >= this.plugin.getServer().getMaxPlayers()) {
            if (!this.plugin.hasPermission(event.getPlayer(), "join-full")) {
                event.setResult(Result.KICK_FULL);
                event.kickMessage(MiniMessage.miniMessage().deserialize("<red>You are not allowed to join full games."));
            } else {
                event.setResult(Result.ALLOWED);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        event.joinMessage(null);

        event.getPlayer().setGameMode(GameMode.ADVENTURE);
        event.getPlayer().teleport(new Location(
                Bukkit.getWorld(this.plugin.getConfig().getString("join-location.world")),
                this.plugin.getConfig().getDouble("join-location.x"),
                this.plugin.getConfig().getDouble("join-location.y"),
                this.plugin.getConfig().getDouble("join-location.z"),
                (float) this.plugin.getConfig().getDouble("join-location.yaw"),
                (float) this.plugin.getConfig().getDouble("join-location.pitch")));

        final GamePlayer gp = new GamePlayer(this.plugin, event.getPlayer());
        this.plugin.currentGame().addPlayer(gp);
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.loadPlayerStats(gp));
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        event.quitMessage(null);

        final GamePlayer gp = this.plugin.currentGame().removePlayer(event.getPlayer(), true);
        if (gp != null) {
            this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.savePlayerStats(gp));
        }
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        final Player player = event.getEntity();
        final int maxHealth = this.plugin.getConfig().getInt("game.player-health");
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        player.setHealth(maxHealth);

        if (this.plugin.currentGame().player(player) != null) {
            this.plugin.getServer().getScheduler().runTaskLater(
                    this.plugin, () -> this.plugin.currentGame().removePlayer(player, false), 1);
        }
    }

    @EventHandler
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        final GamePlayer gp = this.plugin.currentGame().player((Player) event.getEntity());
        if (gp != null && gp.isDead()) {
            event.setCancelled(true);
            return;
        }

        if (this.plugin.currentGame().isGracePeriod() || this.plugin.currentGame().gameState() != GameState.RUNNING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            final GamePlayer gp = this.plugin.currentGame().player((Player) event.getEntity());
            if (gp != null && gp.isDead()) {
                event.setCancelled(true);
                return;
            }
        }

        if (this.plugin.currentGame().isGracePeriod() || this.plugin.currentGame().gameState() != GameState.RUNNING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (this.plugin.currentGame().isGracePeriod()
                || this.plugin.currentGame().gameState() != GameState.RUNNING
                || event.getEntity().getType() == EntityType.PLAYER && event.getDamager().getType() == EntityType.PLAYER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBucketFill(final PlayerBucketFillEvent event) {
        if (event.getItemStack().getType() == Material.LAVA_BUCKET) {
            event.setCancelled(true);
            this.plugin.message(event.getPlayer(), "<gray>You cannot fill your bucket with <red>Lava<gray>.");
        }
    }

    //    @EventHandler
    //    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
    //        plugin.getLogger().info(String.format("item=%s, clicked=%s, block=", event.getItemStack().getType().name(), event.getBlockClicked().getType().name(), event.getBlock().getType().name()));
    ////
    //        GamePlayer gp = plugin.getGame().getPlayer(event.getPlayer());
    //        if (gp != null) {
    //            gp.setBucketUses(gp.getBucketUses() + 1);
    //            plugin.getLogger().info("empty bucket: " + event.getPlayer().getName() + " :: " + gp.getBucketUses());
    //
    //            if (gp.getBucketUses() == 10) {
    //                int slot = event.getPlayer().getInventory().first(Material.WATER_BUCKET);
    //                event.setCancelled(true);
    ////                event.getPlayer().getInventory().setItem(slot, null);
    ////                plugin.getLogger().info("rem " + slot);
    //                plugin.getLogger().info("10 uses");
    //                event.setItemStack(null);
    //            } else {
    //                int slot = event.getPlayer().getInventory().first(Material.WATER_BUCKET);
    //                plugin.getLogger().info("" + slot);
    ////                event.getPlayer().getInventory().remove(Material.BUCKET);
    ////                event.getPlayer().getInventory().remove(Material.WATER_BUCKET);
    ////                event.getPlayer().getInventory().setItem(slot, new ItemStack(Material.WATER_BUCKET));
    //
    //                event.setItemStack( new ItemStack(Material.WATER_BUCKET));
    //            }
    //
    ////            if (event.getHand() == EquipmentSlot.HAND) {
    ////                event.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
    ////                gp.setBucketUses(gp.getBucketUses() - 1);
    ////            } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
    ////                event.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.BUCKET));
    ////                gp.setBucketUses(gp.getBucketUses() - 1);
    ////            }
    //        }
    //    }

    @EventHandler
    public void onPlayerEat(final PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.COOKED_BEEF) {
            final int slot = event.getPlayer().getInventory().first(Material.COOKED_BEEF);
            event.getPlayer().getInventory().setItem(slot,
                    Util.displayName(new ItemStack(Material.COOKED_BEEF), "<green><gold>Infinite Steak Supply"));
        }
    }

}

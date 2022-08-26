package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.util.Util;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public PlayerListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
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

        //final GamePlayer gp = new GamePlayer(this.plugin, event.getPlayer());
        //this.plugin.currentGames().addPlayer(gp);
        //this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.plugin.loadPlayerStats(gp));
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        event.quitMessage(null);

        final GamePlayer gp = this.plugin.gameTracker().removePlayer(event.getPlayer(), true);

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

        if (this.plugin.gameTracker().findPlayer(player) != null) {
            this.plugin.getServer().getScheduler().runTaskLater(
                    this.plugin, () -> this.plugin.gameTracker().removePlayer(player, false), 1);
        }
    }

    @EventHandler
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        final GamePlayer gp = this.plugin.gameTracker().findPlayer((Player) event.getEntity());

        if (gp == null || gp.isDead()) {
            event.setCancelled(true);
            return;
        }

        final Game game = this.plugin.gameTracker().findGame((Player) event.getEntity());

        if (game == null || game.isGracePeriod()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            final GamePlayer gp = this.plugin.gameTracker().findPlayer((Player) event.getEntity());

            if (gp != null && gp.isDead()) {
                event.setCancelled(true);
                return;
            }
        }

        final Game game = this.plugin.gameTracker().findGame(event.getEntity().getWorld());

        if (game == null || game.isGracePeriod()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() == EntityType.PLAYER && event.getDamager().getType() == EntityType.PLAYER) {
            event.setCancelled(true);
        }

        final Game game = this.plugin.gameTracker().findGame(event.getEntity().getWorld());

        if (game == null || game.isGracePeriod() || game.gameState() != GameState.RUNNING) {
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

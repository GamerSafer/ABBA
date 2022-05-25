package com.github.colebennett.abbacaving.listeners;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.GamePlayer;
import com.github.colebennett.abbacaving.game.GameState;
import com.github.colebennett.abbacaving.util.Util;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public PlayerListener(AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (plugin.getGame().getState() == GameState.RUNNING || plugin.getGame().getState() == GameState.DONE) {
            event.setResult(Result.KICK_OTHER);
            event.setKickMessage(ChatColor.RED + "A game is currently in progress.");
            return;
        }

        if (plugin.getGame().getPlayers().size() >= plugin.getServer().getMaxPlayers()) {
            if (!plugin.hasPermission(event.getPlayer(), "join-full")) {
                event.setResult(Result.KICK_FULL);
                event.setKickMessage(ChatColor.RED + "You are not allowed to join full games.");
            } else {
                event.setResult(Result.ALLOWED);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        event.getPlayer().setGameMode(GameMode.ADVENTURE);
        event.getPlayer().teleport(new Location(
            Bukkit.getWorld(plugin.getConfig().getString("join-location.world")),
            plugin.getConfig().getDouble("join-location.x"),
            plugin.getConfig().getDouble("join-location.y"),
            plugin.getConfig().getDouble("join-location.z"),
            (float) plugin.getConfig().getDouble("join-location.yaw"),
            (float) plugin.getConfig().getDouble("join-location.pitch")));

        GamePlayer gp = new GamePlayer(plugin, event.getPlayer());
        plugin.getGame().addPlayer(gp);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.loadPlayerStats(gp));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        
        GamePlayer gp = plugin.getGame().removePlayer(event.getPlayer(), true);
        if (gp != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.savePlayerStats(gp));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player player = event.getEntity();
        int maxHealth = plugin.getConfig().getInt("game.player-health");
        player.setMaxHealth(maxHealth);
        player.setHealth(maxHealth);

        if (plugin.getGame().getPlayer(player) != null) {
            plugin.getServer().getScheduler().runTaskLater(
                    plugin, () -> plugin.getGame().removePlayer(player, false), 1);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        GamePlayer gp = plugin.getGame().getPlayer((Player) event.getEntity());
        if (gp != null && gp.isDead()) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getGame().isGracePeriod() || plugin.getGame().getState() != GameState.RUNNING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            GamePlayer gp = plugin.getGame().getPlayer((Player) event.getEntity());
            if (gp != null && gp.isDead()) {
                event.setCancelled(true);
                return;
            }
        }

        if (plugin.getGame().isGracePeriod() || plugin.getGame().getState() != GameState.RUNNING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (plugin.getGame().isGracePeriod()
                || plugin.getGame().getState() != GameState.RUNNING
                || (event.getEntity().getType() == EntityType.PLAYER && event.getDamager().getType() == EntityType.PLAYER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (event.getItemStack().getType() == Material.LAVA_BUCKET) {
            event.setCancelled(true);
            plugin.message(event.getPlayer(), "<gray>You cannot fill your bucket with <red>Lava<gray>.");
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
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.COOKED_BEEF) {
            int slot = event.getPlayer().getInventory().first(Material.COOKED_BEEF);
            event.getPlayer().getInventory().setItem(slot,
                    Util.setDisplayName(new ItemStack(Material.COOKED_BEEF), "&a&lInfinite Steak Supply"));
        }
    }
}

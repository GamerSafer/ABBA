package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PlayerListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public PlayerListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
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
        final Player player = event.getEntity();
        final GamePlayer gamePlayer = this.plugin.gameTracker().findPlayer(player);

        event.deathMessage(MiniMessage.miniMessage().deserialize(this.plugin.configMessage("death-message"),
                TagResolver.resolver("player", Tag.inserting(event.getPlayer().name())),
                TagResolver.resolver("score", Tag.inserting(Component.text(gamePlayer.score())))));
        event.getDrops().clear();
        event.setDroppedExp(0);

        final @Nullable AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        attribute.setBaseValue(attribute.getDefaultValue());
        player.setHealth(attribute.getBaseValue());

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

    private void handleEntityEvent(final Entity target, final Cancellable cancellable) {
        if (target instanceof Player player) {
            final GamePlayer gp = this.plugin.gameTracker().findPlayer(player);

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

    // TODO: check if this listener is necessary, the above event should cover this
    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        this.handleEntityEvent(event.getEntity(), event);
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

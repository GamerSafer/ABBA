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
import org.bukkit.event.player.PlayerChangedWorldEvent;
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
        this.handleEntityEvent(event.getEntity(), event);
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

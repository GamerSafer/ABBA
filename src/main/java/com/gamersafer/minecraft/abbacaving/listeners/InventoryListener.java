package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public InventoryListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        final Inventory inv = event.getInventory();
        final ItemStack cursorItem = event.getCursor();

        if (inv.getType() == InventoryType.PLAYER || cursorItem == null) {
            return;
        }

        event.setCancelled(true);
        event.setCursor(null);

        final CaveLoot lootItem = this.plugin.lootFromItem(cursorItem.getType());

        if (lootItem == null) {
            return;
        }

        final Player player = (Player) event.getView().getPlayer();
        final GamePlayer gp = this.plugin.gameTracker().findPlayerInGame(player);

        if (gp == null) {
            return;
        }

        final Game game = this.plugin.gameTracker().findGame(player);

        gp.addScore(lootItem.value(), lootItem.name());
        game.increasePlayerScore(gp, lootItem.value());

        // game.broadcast(this.plugin.configMessage("player-found-item"), Map.of(
        //         "player", player.displayName(),
        //         "item", Component.text(lootItem.name()),
        //         "article", Component.text(lootItem.article().isEmpty() ? "" : lootItem.article() + " ")
        // ));
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        final Inventory inv = event.getClickedInventory();
        final ItemStack currentItem = event.getCurrentItem();

        if (inv == null || currentItem == null) {
            return;
        }

        event.setCancelled(true);

        final CaveLoot lootItem = this.plugin.lootFromItem(currentItem.getType());

        if (lootItem == null) {
            return;
        }

        final Player player = (Player) event.getView().getPlayer();
        final GamePlayer gp = this.plugin.gameTracker().findPlayerInGame(player);

        if (gp == null) {
            return;
        }

        final Game game = this.plugin.gameTracker().findGame(player);

        gp.addScore(lootItem.value(), lootItem.name());
        game.increasePlayerScore(gp, lootItem.value());

        event.setCurrentItem(null);

        // TODO: Don't broadcast message for all items, if a chest has 7 items, that will be 7 messages in chat.
        // game.broadcast(this.plugin.configMessage("player-found-item"), Map.of(
        //         "player", player.displayName(),
        //         "item", Component.text(lootItem.name()),
        //         "article", Component.text(lootItem.article().isEmpty() ? "" : lootItem.article() + " ")
        // ));
    }

}

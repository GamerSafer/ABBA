package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class InventoryListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public InventoryListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        final Inventory inv = event.getClickedInventory();

        if (inv == null || inv.getType() == InventoryType.PLAYER || event.getCurrentItem() != null) {
            return;
        }

        event.setCurrentItem(null);
        event.setCancelled(true);

        final CaveLoot lootItem = this.plugin.lootFromItem(event.getCurrentItem().getType());

        if (lootItem == null) {
            return;
        }

        final Player player = (Player) event.getView().getPlayer();
        final GamePlayer gp = this.plugin.gameTracker().findPlayer(player);

        if (gp == null) {
            return;
        }

        final Game game = this.plugin.gameTracker().findGame(player);

        gp.addScore(lootItem.value(), lootItem.name());
        game.increasePlayerScore(gp, lootItem.value());

        game.broadcast(this.plugin.configMessage("player-found-item"), Map.of(
                "player", player.displayName(),
                "item", Component.text(lootItem.name()),
                "article", Component.text(lootItem.article().isEmpty() ? "" : lootItem.article() + " ")
        ));
    }

}

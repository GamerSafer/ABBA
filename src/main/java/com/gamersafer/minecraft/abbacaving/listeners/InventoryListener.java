package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
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
        if (inv != null && inv.getType() != InventoryType.PLAYER) {
            if (event.getCurrentItem() != null) {
                final CaveLoot lootItem = this.plugin.lootFromItem(event.getCurrentItem().getType());
                if (lootItem != null) {
                    final Player player = (Player) event.getView().getPlayer();
                    final GamePlayer gp = this.plugin.currentGame().player(player);
                    if (gp != null) {
                        event.setCurrentItem(null);
                        event.setCancelled(true);

                        gp.addScore(lootItem.value(), lootItem.name());

                        this.plugin.broadcast(this.plugin.configMessage("player-found-item"), Map.of(
                                "player", player.displayName(),
                                "item", Component.text(lootItem.name()),
                                "article", Component.text(lootItem.article().isEmpty() ? "" : lootItem.article() + " ")
                        ));
                    }
                }
            }
        }
    }

}

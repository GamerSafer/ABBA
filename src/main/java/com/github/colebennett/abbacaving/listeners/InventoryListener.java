package com.github.colebennett.abbacaving.listeners;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.CaveLoot;
import com.github.colebennett.abbacaving.game.GamePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class InventoryListener implements Listener {

    private final AbbaCavingPlugin plugin;

    public InventoryListener(AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        if (inv != null && inv.getType() != InventoryType.PLAYER) {
            if (event.getCurrentItem() != null) {
                CaveLoot lootItem = plugin.lootFromItem(event.getCurrentItem().getType());
                if (lootItem != null) {
                    Player player = (Player) event.getView().getPlayer();
                    GamePlayer gp = plugin.getGame().getPlayer(player);
                    if (gp != null) {
                        event.setCurrentItem(null);
                        event.setCancelled(true);

                        gp.addScore(lootItem.getValue(), lootItem.getName());

                        plugin.broadcast(plugin.getMessage("player-found-item"), new HashMap<>() {{
                            put("player", player.displayName());
                            put("item", Component.text(lootItem.getName()));
                            put("article", Component.text(lootItem.getArticle().isEmpty() ? "" : lootItem.getArticle() + " "));
                        }});
                    }
                }
            }
        }
    }
}

package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.util.Sounds;
import com.gamersafer.minecraft.abbacaving.util.Util;
import java.util.Map;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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

        gp.gameStats().addScore(lootItem.value(), lootItem.name());
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

        final HumanEntity clicker = event.getWhoClicked();
        if (clicker.hasPermission("abbacaving.build")) {
            return;
        }

        if (this.handleInventoryMenu(event)) {
            event.setCancelled(true);
            return;
        }

        if (this.isHotbarClick(event)) {
            return;
        }

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

        gp.gameStats().addScore(lootItem.value(), lootItem.name());
        game.increasePlayerScore(gp, lootItem.value());

        event.setCurrentItem(null);
    }

    private boolean isHotbarClick(final InventoryClickEvent event) {
        return event.getSlotType() == InventoryType.SlotType.QUICKBAR;
    }

    private boolean handleInventoryMenu(final InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof PlayerInventory)) {
            return false;
        }

        final Player player = (Player) event.getView().getPlayer();

        // 19 21 23 25
        switch (event.getSlot()) {
            case 19 -> this.saveLayoutButton(player, event.isShiftClick());
            case 21 -> this.plugin.cosmeticGui().showCosmeticsGUI(player);
            case 23 -> this.showStats(player);
            case 25 -> this.returnToLobby(player);
        }

        return event.getSlot() >= 9 && event.getSlot() <= 35;
    }

    private void saveLayoutButton(final Player player, final boolean isShiftClick) {
        if (!player.hasPermission("abbacaving.hotbar")) {
            this.plugin.message(player, this.plugin.configMessage("no-permission"));
            return;
        }

        final GamePlayer gamePlayer = this.plugin.gameTracker().findPlayerInGame(player);
        final Game game = this.plugin.gameTracker().findGame(player);

        if (game != null && gamePlayer != null) {
            if (isShiftClick) {
                gamePlayer.hotbarLayout().clear();
            } else {
                gamePlayer.populateHotbar();
            }

            gamePlayer.saveHotbar();
            if (isShiftClick) {
                this.plugin.message(player, this.plugin.configMessage("layout-default-saved"));
            } else {
                this.plugin.message(player, this.plugin.configMessage("layout-saved"));
            }
            Sounds.pling(player);
        } else {
            this.plugin.message(player, "<red>Failed to save hotbar layout!");
        }

    }

    private void showStats(final Player player) {
        player.closeInventory();
        Sounds.pling(player);
        final GamePlayer gamePlayer = this.plugin.gameTracker().gamePlayer(player);
        final Game game = gamePlayer.gameStats().game();

        this.plugin.message(player, this.plugin.configMessage("stats-own"));
        this.plugin.message(player, "");
        this.plugin.message(player, this.plugin.configMessage("stats-all-time"));
        this.plugin.message(player, this.plugin.configMessage("stats-wins"), Map.of("wins", Util.addCommas(gamePlayer.wins())));
        this.plugin.message(player, this.plugin.configMessage("stats-score"), Map.of("score", Util.addCommas(gamePlayer.highestScore())));
        this.plugin.message(player, this.plugin.configMessage("stats-ores"), Map.of("ores", Util.addCommas(gamePlayer.totalOresMined())));
        this.plugin.message(player, "");
        this.plugin.message(player, this.plugin.configMessage("stats-in-game"));
        this.plugin.message(player, this.plugin.configMessage("stats-in-game-map"), Map.of("map", game.mapName()));
        this.plugin.message(player, this.plugin.configMessage("stats-in-game-score"), Map.of("score", Integer.toString(gamePlayer.gameStats().score())));
        this.plugin.message(player, this.plugin.configMessage("stats-in-game-ores"), Map.of("ores", Integer.toString(gamePlayer.gameStats().currentOresMined())));
    }

    private void returnToLobby(final Player player) {
        final Game game = this.plugin.gameTracker().findGame(player);

        if (game != null) {
            game.removePlayer(player, true);
            this.plugin.lobby().sendToLobby(player);
        }
    }

}

package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.player.GameStats;
import com.gamersafer.minecraft.abbacaving.tools.ToolManager;
import com.gamersafer.minecraft.abbacaving.util.Messages;
import com.gamersafer.minecraft.abbacaving.util.Sounds;
import com.gamersafer.minecraft.abbacaving.util.Stats;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

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

        final CaveLoot lootItem = this.plugin.getLootHandler().lootFromItem(cursorItem.getType());

        if (lootItem == null) {
            return;
        }

        final Player player = (Player) event.getView().getPlayer();
        final Game game = this.plugin.gameTracker().findGame(player);

        if (game == null) {
            event.setCancelled(true);
            return;
        }

        final GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player);
        GameStats stats = game.getGameData(gamePlayer);

        int score = lootItem.value() * cursorItem.getAmount();
        stats.addScore(score, lootItem.name());
        game.increasePlayerScore(gamePlayer, score);

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
        // Only if player is not in a game
        if (clicker.hasPermission("abbacaving.build") && this.plugin.gameTracker().findGame((Player) clicker) == null) {
            return;
        }

        if (this.handleInventoryMenu(event)) {
            event.setCancelled(true);
            return;
        }

        if (this.isHotbarClick(event)) {
            return;
        }

        InventoryHolder holder = event.getClickedInventory().getHolder();
        if (holder instanceof BlockInventoryHolder || holder instanceof Entity) {
            final CaveLoot lootItem = this.plugin.getLootHandler().lootFromItem(currentItem.getType());

            if (lootItem == null) {
                event.setCancelled(true);
                return;
            }

            final Player player = (Player) event.getView().getPlayer();
            final Game game = this.plugin.gameTracker().findGame(player);

            if (game == null) {
                return;
            }
            final GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player);
            GameStats stats = game.getGameData(gamePlayer);

            int score = lootItem.value() * currentItem.getAmount();
            stats.addScore(score, lootItem.name());
            game.increasePlayerScore(gamePlayer, score);

            event.setCurrentItem(null);
        }

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
            Messages.message(player, this.plugin.configMessage("no-permission"));
            return;
        }

        final Game game = this.plugin.gameTracker().findGame(player);
        if (game != null) {
            final GamePlayer gamePlayer = this.plugin.getPlayerCache().getLoaded(player);
            if (isShiftClick) {
                gamePlayer.data().getHotbarLayout().clear();
            } else {
                gamePlayer.data().setHotbarLayout(ToolManager.serializeHotbarTools(player));
            }

            gamePlayer.data().saveHotbar();
            if (isShiftClick) {
                Messages.message(player, this.plugin.configMessage("layout-default-saved"));
            } else {
                Messages.message(player, this.plugin.configMessage("layout-saved"));
            }
            Sounds.pling(player);
        } else {
            Messages.message(player, "<red>Failed to save hotbar layout!");
        }

    }

    private void showStats(final Player player) {
        player.closeInventory();
        Sounds.pling(player);

        Game game = this.plugin.gameTracker().findGame(player);
        if (game != null) {
            Stats.dumpGameStats(player, game, game.getGameData(this.plugin.getPlayerCache().getLoaded(player)));
        }
    }

    private void returnToLobby(final Player player) {
        final Game game = this.plugin.gameTracker().findGame(player);

        if (game != null) {
            game.playerChosenDisconnect(player);
        }
    }

}

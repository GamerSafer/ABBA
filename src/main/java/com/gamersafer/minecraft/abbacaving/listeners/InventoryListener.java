package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.util.ItemBuilder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
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
    private final ChestGui cosmeticsGui;

    public InventoryListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
        this.cosmeticsGui = new ChestGui(6, "Cosmetics");
        this.setupGui();
    }

    private void setupGui() {
        final StaticPane outlinePane = new StaticPane(0, 0, 9, 6);
        outlinePane.fillWith(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        outlinePane.setPriority(Pane.Priority.LOWEST);
        this.cosmeticsGui.addPane(outlinePane);

        final StaticPane inlinePane = new StaticPane(1, 1, 7, 4);
        inlinePane.fillWith(new ItemStack(Material.GREEN_STAINED_GLASS_PANE));
        inlinePane.setPriority(Pane.Priority.LOW);
        this.cosmeticsGui.addPane(inlinePane);

        final StaticPane contentPane = new StaticPane(0, 0, 9, 6);
        contentPane.setPriority(Pane.Priority.HIGHEST);

        // TODO: info book

        final GuiItem resetArmor = new GuiItem(new ItemBuilder(Material.IRON_CHESTPLATE)
                .name(Component.text("Reset Armor Cosmetics")).build(), event -> {
            // TODO: reset armor
        });

        contentPane.addItem(resetArmor, 2, 2);

        contentPane.addItem(new GuiItem(new ItemStack(Material.STONE)), 4, 2);

        final GuiItem resetWeapon = new GuiItem(new ItemBuilder(Material.STONE_SWORD)
                .name(Component.text("Reset Weapon Cosmetics")).build(), event -> {
            // TODO: reset weapon
        });

        contentPane.addItem(resetWeapon, 6, 2);

        final GuiItem armorCosmetics = new GuiItem(new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                .name(Component.text("Armor Cosmetics")).build(), event -> {
            // TODO: open armor cosmetics menu
        });

        contentPane.addItem(armorCosmetics, 2, 3);

        contentPane.addItem(new GuiItem(new ItemStack(Material.STONE)), 4, 3);

        final GuiItem weaponCosmetics = new GuiItem(new ItemBuilder(Material.GOLDEN_SWORD)
                .name(Component.text("Weapon Cosmetics")).build(), event -> {
            // TODO: open weapon cosmetics menu
        });

        contentPane.addItem(weaponCosmetics, 6, 3);

        this.cosmeticsGui.addPane(contentPane);
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

        if (event.getWhoClicked().hasPermission("abbacaving.inventory")) {
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
            return true;
        }

        final Player player = (Player) event.getView().getPlayer();

        // 19 21 23 25
        switch (event.getSlot()) {
            case 19 -> this.saveLayoutButton(player, event.isShiftClick());
            case 21 -> this.openCosmeticsMenu(player);
            case 23 -> this.showStats(player);
            case 25 -> this.returnToLobby(player);
        }

        return event.getSlot() >= 9 && event.getSlot() <= 35;
    }

    private void saveLayoutButton(final Player player, final boolean isShiftClick) {
        final GamePlayer gamePlayer = this.plugin.gameTracker().findPlayerInGame(player);
        final Game game = this.plugin.gameTracker().findGame(player);

        if (game != null && gamePlayer != null) {
            if (isShiftClick) {
                game.applyDefaultHotbar(gamePlayer); // TODO: message "hotbar reset"
            }

            game.saveHotbar(gamePlayer);
            this.plugin.message(player, this.plugin.configMessage("layout-saved"));
        } else {
            this.plugin.message(player, "<red>Failed to save hotbar layout!");
        }

    }

    private void openCosmeticsMenu(final Player player) {
        this.cosmeticsGui.show(player);
    }

    private void showStats(final Player player) {
        player.performCommand("/abbacaving:stats");
    }

    private void returnToLobby(final Player player) {
        final Game game = this.plugin.gameTracker().findGame(player);

        if (game != null) {
            game.removePlayer(player, true);
            game.sendToLobby(player);
        }
    }

}

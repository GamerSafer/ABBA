package com.gamersafer.minecraft.abbacaving.listeners;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.util.ItemBuilder;
import com.gamersafer.minecraft.abbacaving.util.Sounds;
import com.gamersafer.minecraft.abbacaving.util.Util;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryListener implements Listener {

    private final AbbaCavingPlugin plugin;
    private final ChestGui cosmeticsGui;

    public InventoryListener(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
        this.cosmeticsGui = new ChestGui(6, "Cosmetics");
        this.setupGui();
    }

    private void setupGui() {
        final StaticPane contentPane = this.setupCosmeticsGui(this.cosmeticsGui);

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
            final ChestGui armorGui = new ChestGui(5, "Armor Cosmetics");

            final StaticPane armorContentPane = this.setupCosmeticsGui(armorGui);

            final ConfigurationSection armorConfig = this.plugin.getConfig().getConfigurationSection("cosmetics.armor");

            int x = 2;
            final int y = 2;

            for (final String key : armorConfig.getKeys(false)) {
                final ConfigurationSection armor = armorConfig.getConfigurationSection(key);

                final String permission = Objects.requireNonNullElse(armor.getString("permission"), "abbacaving.armor." + key);

                if (event.getWhoClicked().hasPermission(permission)) {
                    final Material material = Material.getMaterial(armor.getString("material"));
                    final Component displayName = MiniMessage.miniMessage().deserialize(armor.getString("display_name"));
                    final ItemStack armorItem = new ItemStack(material);
                    final List<Component> lore = new ArrayList<>();

                    if (armor.contains("lore")) {
                        for (final String loreEntry : armor.getStringList("lore")) {
                            lore.add(MiniMessage.miniMessage().deserialize(loreEntry));
                        }
                    }

                    armorItem.lore(lore);

                    final ItemMeta itemMeta = armorItem.getItemMeta();
                    itemMeta.displayName(displayName);

                    if (armor.contains("custom_model_data")) {
                        itemMeta.setCustomModelData(armor.getInt("custom_model_data"));
                    }

                    armorItem.setItemMeta(itemMeta);

                    armorContentPane.addItem(new GuiItem(armorItem, cosmeticClick -> {
                        // TODO: apply cosmetic
                    }), x++, y);
                }
            }

            armorGui.show(event.getWhoClicked());
        });

        contentPane.addItem(armorCosmetics, 2, 3);

        contentPane.addItem(new GuiItem(new ItemStack(Material.STONE)), 4, 3);

        final GuiItem weaponCosmetics = new GuiItem(new ItemBuilder(Material.GOLDEN_SWORD)
                .name(Component.text("Weapon Cosmetics")).build(), event -> {
            final ChestGui weaponGui = new ChestGui(5, "Weapon Cosmetics");

            final StaticPane weaponContentPane = this.setupCosmeticsGui(weaponGui);

            final ConfigurationSection weapons = this.plugin.getConfig().getConfigurationSection("cosmetics.weapons");

            int x = 2;
            final int y = 2;

            for (final String key : weapons.getKeys(false)) {
                final ConfigurationSection weapon = weapons.getConfigurationSection(key);

                final String permission = Objects.requireNonNullElse(weapon.getString("permission"), "abbacaving.weapon." + key);

                if (event.getWhoClicked().hasPermission(permission)) {
                    final Material material = Material.getMaterial(weapon.getString("material"));
                    final Component displayName = MiniMessage.miniMessage().deserialize(weapon.getString("display_name"));
                    final ItemStack weaponItem = new ItemStack(material);
                    final List<Component> lore = new ArrayList<>();

                    if (weapon.contains("lore")) {
                        for (final String loreEntry : weapon.getStringList("lore")) {
                            lore.add(MiniMessage.miniMessage().deserialize(loreEntry));
                        }
                    }

                    weaponItem.lore(lore);

                    final ItemMeta itemMeta = weaponItem.getItemMeta();
                    itemMeta.displayName(displayName);

                    if (weapon.contains("custom_model_data")) {
                        itemMeta.setCustomModelData(weapon.getInt("custom_model_data"));
                    }

                    weaponItem.setItemMeta(itemMeta);

                    weaponContentPane.addItem(new GuiItem(weaponItem, cosmeticClick -> {
                        // TODO: apply cosmetic
                    }), x++, y);
                }
            }

            weaponGui.show(event.getWhoClicked());
        });

        contentPane.addItem(weaponCosmetics, 6, 3);

        this.cosmeticsGui.addPane(contentPane);
    }

    private StaticPane setupCosmeticsGui(final ChestGui gui) {
        final StaticPane outlinePane = new StaticPane(0, 0, 9, gui.getRows());
        outlinePane.fillWith(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        outlinePane.setPriority(Pane.Priority.LOWEST);
        gui.addPane(outlinePane);

        final StaticPane inlinePane = new StaticPane(1, 1, 7, gui.getRows() - 2);
        inlinePane.fillWith(new ItemStack(Material.GREEN_STAINED_GLASS_PANE));
        inlinePane.setPriority(Pane.Priority.LOW);
        gui.addPane(inlinePane);

        final StaticPane contentPane = new StaticPane(0, 0, 9, gui.getRows());
        contentPane.setPriority(Pane.Priority.HIGHEST);

        gui.addPane(contentPane);

        return contentPane;
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

        HumanEntity clicker = event.getWhoClicked();
        if (clicker.hasPermission("abbacaving.build")) {
            return;
        }

        int slot = event.getSlot();
        if (slot < 9 && clicker.hasPermission("abbacaving.hotbar")) {
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
        if (!player.hasPermission("abbacaving.hotbar")) {
            this.plugin.message(player, this.plugin.configMessage("no-permission"));
            return;
        }

        final GamePlayer gamePlayer = this.plugin.gameTracker().findPlayerInGame(player);
        final Game game = this.plugin.gameTracker().findGame(player);

        if (game != null && gamePlayer != null) {
            if (isShiftClick) {
                game.applyDefaultHotbar(gamePlayer); // TODO: message "hotbar reset"
            }

            game.saveHotbar(gamePlayer);
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

    private void openCosmeticsMenu(final Player player) {
        this.cosmeticsGui.show(player);
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

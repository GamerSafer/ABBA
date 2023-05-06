package com.gamersafer.minecraft.abbacaving.guis;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.tools.ToolSpecies;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.tools.CosmeticRegistry;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.util.Components;
import com.gamersafer.minecraft.abbacaving.util.Messages;
import com.gamersafer.minecraft.abbacaving.util.Sounds;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

public class CosmeticGui {

    private final AbbaCavingPlugin plugin;
    private final ChestGui cosmeticsGui;

    public CosmeticGui(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;

        final List<GuiItem> items = new ArrayList<>();
        for (final ToolSpecies species : ToolSpecies.values()) {

            final ChestGui speciesGui = this.createSpeciesGui(species);
            items.add(new GuiItem(ToolSpecies.display(species), inventoryClickEvent -> {
                final HumanEntity clicker = inventoryClickEvent.getWhoClicked();
                speciesGui.show(clicker);
                Sounds.choose(clicker);
            }));
        }

        this.cosmeticsGui = new SimpleListGui(items, Components.plainText("Cosmetics"), null);
    }

    public void showCosmeticsGUI(final Player player) {
        this.cosmeticsGui.show(player);
    }

    public ChestGui createSpeciesGui(final ToolSpecies species) {
        final List<GuiItem> items = new ArrayList<>();
        for (final ToolType type : species.types()) {
            final ChestGui toolGui = this.createToolGui(type);
            items.add(new GuiItem(type.icon(), inventoryClickEvent -> {
                final HumanEntity clicker = inventoryClickEvent.getWhoClicked();
                toolGui.show(clicker);
                Sounds.choose(clicker);
            }));
        }

        return new SimpleListGui(items, Components.plainText(species.name()), event -> {
            this.showCosmeticsGUI((Player) event.getWhoClicked());
        });
    }

    public ChestGui createToolGui(final ToolType type) {
        final List<GuiItem> items = new ArrayList<>();

        items.add(new GuiItem(type.icon(), event -> {
            final GamePlayer gamePlayer = this.plugin.gameTracker().gamePlayer(event.getWhoClicked().getUniqueId());

            Messages.message(gamePlayer.player(), this.plugin.configMessage("cosmetic-select"), Map.of("cosmetic", "default"));
            gamePlayer.data().removeSelectedCosmetic(type);
            if (gamePlayer.gameStats() != null && gamePlayer.gameStats().game().gameState() == GameState.RUNNING) {
                Messages.message(gamePlayer.player(), this.plugin.configMessage("cosmetic-apply-after-game"));
            }
        }));

        for (final CosmeticRegistry.Cosmetic cosmetic : this.plugin.cosmeticRegistry().get(type)) {
            items.add(new GuiItem(cosmetic.itemStack(), inventoryClickEvent -> {
                final GamePlayer gamePlayer = this.plugin.gameTracker().gamePlayer(inventoryClickEvent.getWhoClicked().getUniqueId());
                if (!gamePlayer.player().hasPermission(cosmetic.permission())) {
                    Messages.message(gamePlayer.player(), this.plugin.configMessage("no-permission"));
                    return;
                }

                final CosmeticRegistry.Cosmetic old = gamePlayer.data().removeSelectedCosmetic(cosmetic.toolType());
                if (old != null) {
                    Messages.message(gamePlayer.player(), this.plugin.configMessage("cosmetic-deselect"), Map.of("cosmetic", old.identifier()));
                }
                if (old != cosmetic) {
                    gamePlayer.data().addSelectedCosmetic(type, cosmetic);
                    Messages.message(gamePlayer.player(), this.plugin.configMessage("cosmetic-select"), Map.of("cosmetic", cosmetic.identifier()));
                }

                if (gamePlayer.gameStats() != null) {
                    if (gamePlayer.gameStats().game().gameState() == GameState.RUNNING) {
                        Messages.message(gamePlayer.player(), this.plugin.configMessage("cosmetic-apply-after-game"));
                    }
                }
            }));
        }

        return new SimpleListGui(items, Components.plainText("Cosmetics"), event -> {
            this.showCosmeticsGUI((Player) event.getWhoClicked());
        });
    }

}

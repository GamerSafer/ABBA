package com.gamersafer.minecraft.abbacaving.guis;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.commands.ToolSpecies;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.GameState;
import com.gamersafer.minecraft.abbacaving.tools.CosmeticRegistry;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.util.Components;
import com.gamersafer.minecraft.abbacaving.util.ItemUtil;
import com.gamersafer.minecraft.abbacaving.util.Sounds;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CosmeticGui {

    private final AbbaCavingPlugin plugin;
    private final ChestGui cosmeticsGui;

    public CosmeticGui(AbbaCavingPlugin plugin) {
        this.plugin = plugin;

        List<GuiItem> items = new ArrayList<>();
        for (ToolSpecies species : ToolSpecies.values()) {

            ChestGui speciesGui = this.createSpeciesGui(species);
            items.add(new GuiItem(ToolSpecies.display(species), inventoryClickEvent -> {
                HumanEntity clicker = inventoryClickEvent.getWhoClicked();
                speciesGui.show(clicker);
                Sounds.choose(clicker);
            }));
        }

        this.cosmeticsGui = new SimpleListGui(items, Components.plainText("Cosmetics"), null);
    }


    public void showCosmeticsGUI(final Player player) {
        this.cosmeticsGui.show(player);
    }


    public ChestGui createSpeciesGui(ToolSpecies species) {
        List<GuiItem> items = new ArrayList<>();
        for (ToolType type : species.getTypes()) {
            ChestGui toolGui = this.createToolGui(type);
            items.add(new GuiItem(type.getIcon(), inventoryClickEvent -> {
                HumanEntity clicker = inventoryClickEvent.getWhoClicked();
                toolGui.show(clicker);
                Sounds.choose(clicker);
            }));
        }

        return new SimpleListGui(items, Components.plainText(species.name()), (event) -> {
            this.showCosmeticsGUI((Player) event.getWhoClicked());
        });
    }

    public ChestGui createToolGui(ToolType type) {
        List<GuiItem> items = new ArrayList<>();

        items.add(new GuiItem(type.getIcon(), (event) -> {
            final GamePlayer gamePlayer = plugin.gameTracker().gamePlayer(event.getWhoClicked().getUniqueId());

            plugin.message(gamePlayer.player(), plugin.configMessage("cosmetic-select"), Map.of("cosmetic", "default"));
            gamePlayer.removeSelectedCosmetic(type);
            if (gamePlayer.gameStats().game().gameState() == GameState.RUNNING) {
                plugin.message(gamePlayer.player(), plugin.configMessage("cosmetic-apply-after-game"));
            }
        }));

        for (CosmeticRegistry.Cosmetic cosmetic : this.plugin.getCosmeticRegistry().get(type)) {
            items.add(new GuiItem(cosmetic.itemStack(), inventoryClickEvent -> {
                final GamePlayer gamePlayer = plugin.gameTracker().gamePlayer(inventoryClickEvent.getWhoClicked().getUniqueId());
                if (!gamePlayer.player().hasPermission(cosmetic.permission())) {
                    plugin.message(gamePlayer.player(), plugin.configMessage("no-permission"));
                    return;
                }

                CosmeticRegistry.Cosmetic old = gamePlayer.removeSelectedCosmetic(cosmetic.toolType());
                if (old != null) {
                    plugin.message(gamePlayer.player(), plugin.configMessage("cosmetic-deselect"), Map.of("cosmetic", old.identifier()));
                }
                if (old != cosmetic) {
                    gamePlayer.addSelectedCosmetic(type, cosmetic);
                    plugin.message(gamePlayer.player(), plugin.configMessage("cosmetic-select"), Map.of("cosmetic", cosmetic.identifier()));
                }

                if (gamePlayer.gameStats() != null) {
                    if (gamePlayer.gameStats().game().gameState() == GameState.RUNNING) {
                        plugin.message(gamePlayer.player(), plugin.configMessage("cosmetic-apply-after-game"));
                    }
                }
            }));
        }

        return new SimpleListGui(items, Components.plainText("Cosmetics"), (event) -> {
            this.showCosmeticsGUI((Player) event.getWhoClicked());
        });
    }

}

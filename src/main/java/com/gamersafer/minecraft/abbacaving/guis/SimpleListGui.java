package com.gamersafer.minecraft.abbacaving.guis;

import com.gamersafer.minecraft.abbacaving.util.ItemUtil;
import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class SimpleListGui extends ChestGui {

    public SimpleListGui(@NotNull List<GuiItem> elements, @NotNull Component title, @Nullable Consumer<InventoryClickEvent> back) {
        super(calculateRows(elements, back), ComponentHolder.of(title));
        this.setOnTopClick(event -> event.setCancelled(true));

        InventoryComponent inventoryComponent = this.getInventoryComponent();
        StaticPane pane = ItemUtil.wrapGui(inventoryComponent, inventoryComponent.getLength(), this.getRows());
        ItemUtil.listItems(pane, elements);
        if (back != null) {
            pane.addItem(ItemUtil.back(back), pane.getLength() - 1, 0);
        }
    }

    private static int calculateRows(@NotNull List<GuiItem> elements, @Nullable Consumer<InventoryClickEvent> parent) {
        int items = elements.size();
        if (parent != null) {
            items += 1; // Add room for back arrow
        }

        return (int) Math.ceil(items / 9f);
    }

}

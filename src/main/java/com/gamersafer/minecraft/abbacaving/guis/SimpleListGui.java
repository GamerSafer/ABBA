package com.gamersafer.minecraft.abbacaving.guis;

import com.gamersafer.minecraft.abbacaving.util.ItemUtil;
import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SimpleListGui extends ChestGui {

    public SimpleListGui(@NotNull List<GuiItem> elements, @NotNull Component title) {
        super(calculateRows(elements), ComponentHolder.of(title));
        this.setOnTopClick(event -> event.setCancelled(true));

        InventoryComponent inventoryComponent = this.getInventoryComponent();
        StaticPane pane = ItemUtil.wrapGui(inventoryComponent, inventoryComponent.getLength(), this.getRows());
        ItemUtil.listItems(pane, elements);
    }

    private static int calculateRows(@NotNull List<GuiItem> elements) {
        return (int) Math.max(1, Math.ceil(elements.size() / 9f));
    }

}

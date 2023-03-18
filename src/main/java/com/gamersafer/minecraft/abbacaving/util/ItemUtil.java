package com.gamersafer.minecraft.abbacaving.util;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public class ItemUtil {

    public static ItemStack wrapEdit(ItemStack itemStack, Consumer<ItemMeta> meta) {
        itemStack.editMeta(meta);
        return itemStack;
    }

    // Panes are annoying, wrap it
    public static StaticPane wrapGui(InventoryComponent component) {
        return wrapGui(component, component.getLength(), component.getHeight());
    }

    public static StaticPane wrapGui(InventoryComponent component, int length, int height) {
        StaticPane staticPane = new StaticPane(0, 0, length, height);
        component.addPane(staticPane);
        return staticPane;
    }

    public static StaticPane wrapGui(InventoryComponent component, int x, int y, int length, int height) {
        StaticPane staticPane = new StaticPane(x, y, length, height);
        component.addPane(staticPane);
        return staticPane;
    }


    public static StaticPane listItems(StaticPane pane, List<GuiItem> item) {
        Deque<GuiItem> queue = new ArrayDeque<>(item);

        for (int height = 0; height < pane.getHeight(); height++) {
            for (int length = 0; length < pane.getLength(); length++) {
                if (queue.isEmpty()) {
                    return pane;
                }
                GuiItem popped = queue.pop();

                pane.addItem(popped, length, height);
            }
        }

        return pane;
    }

    public static GuiItem back(Inventory inventory) {
        if (inventory.getHolder() instanceof Gui gui) {
            return new GuiItem(BACK, (event) -> {
                gui.show(event.getWhoClicked());
            });
        } else {
            return new GuiItem(new ItemStack(Material.BARRIER));
        }
    }

    public static final ItemStack BACK = new ItemBuilder(Material.ARROW)
            .name(Components.plainText("Back"))
            .build();

}

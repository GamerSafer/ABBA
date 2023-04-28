package com.gamersafer.minecraft.abbacaving.util;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemUtil {

    private ItemUtil() {

    }

    public static ItemStack wrapEdit(final ItemStack itemStack, final Consumer<ItemMeta> meta) {
        itemStack.editMeta(meta);
        return itemStack;
    }

    // Panes are annoying, wrap it
    public static StaticPane wrapGui(final InventoryComponent component) {
        return wrapGui(component, component.getLength(), component.getHeight());
    }

    public static StaticPane wrapGui(final InventoryComponent component, final int length, final int height) {
        final StaticPane staticPane = new StaticPane(0, 0, length, height);
        component.addPane(staticPane);
        return staticPane;
    }

    public static StaticPane wrapGui(final InventoryComponent component, final int x, final int y, final int length, final int height) {
        final StaticPane staticPane = new StaticPane(x, y, length, height);
        component.addPane(staticPane);
        return staticPane;
    }

    public static StaticPane listItems(final StaticPane pane, final List<GuiItem> item) {
        final Deque<GuiItem> queue = new ArrayDeque<>(item);

        for (int height = 0; height < pane.getHeight(); height++) {
            for (int length = 0; length < pane.getLength(); length++) {
                if (queue.isEmpty()) {
                    return pane;
                }
                final GuiItem popped = queue.pop();

                pane.addItem(popped, length, height);
            }
        }

        return pane;
    }

    public static GuiItem back(final Consumer<InventoryClickEvent> runnable) {
        return new GuiItem(BACK, runnable::accept);
    }

    public static final ItemStack BACK = new ItemBuilder(Material.ARROW)
            .name(Components.plainText("Back"))
            .build();

}

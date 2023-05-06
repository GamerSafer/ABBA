package com.gamersafer.minecraft.abbacaving.tools.impl;

import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.tools.impl.resolver.ItemResolver;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class SlottedHotbarTool implements ToolType {

    private static final Map<String, SlottedHotbarTool> REGISTRY = new HashMap<>();
    private static final NamespacedKey KEY = NamespacedKey.fromString("abba:slotted_hotbar_item");

    private final String identifier;
    private final ItemResolver resolver;
    private final int defaultSlot;

    private final boolean isInfinite;

    public SlottedHotbarTool(final String name, final ItemResolver resolver, final int slot) {
        this(name, resolver, slot, false);
    }

    public SlottedHotbarTool(final String name, final ItemResolver resolver, final int slot, final boolean isInfinite) {
        this.identifier = name;
        this.resolver = resolver;
        this.defaultSlot = slot;
        this.isInfinite = isInfinite;
        REGISTRY.put(name, this);
    }

    @Nullable
    public static SlottedHotbarTool stored(final String identifier) {
        return REGISTRY.get(identifier);
    }

    @Nullable
    public static SlottedHotbarTool stored(final ItemStack slotItem) {
        if (slotItem == null) {
            return null;
        }

        final ItemMeta meta = slotItem.getItemMeta();
        if (meta == null) {
            return null;
        }
        final String identifier = meta.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        return stored(identifier);
    }

    @Override
    public void apply(final GamePlayer player) {
        Integer slot = player.data().getHotbarLayout().get(this);
        if (slot == null) {
            slot = this.defaultSlot;
        }

        final ItemStack itemStack = this.resolver.get(player);
        itemStack.editMeta(meta -> {
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, this.identifier);
            meta.setUnbreakable(true);
        });

        player.player().getInventory().setItem(slot, itemStack);
    }

    @Override
    public ItemStack icon() {
        return this.resolver.get(null);
    }

    public String identifier() {
        return this.identifier;
    }

    public boolean isInfinite() {
        return this.isInfinite;
    }

    @Override
    public String toString() {
        return "SlottedHotbarTool{" +
                "identifier='" + this.identifier + '\'' +
                ", resolver=" + this.resolver +
                ", defaultSlot=" + this.defaultSlot +
                ", isInfinite=" + this.isInfinite +
                '}';
    }

}

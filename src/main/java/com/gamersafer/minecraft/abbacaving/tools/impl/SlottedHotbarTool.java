package com.gamersafer.minecraft.abbacaving.tools.impl;

import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.tools.impl.resolver.ItemResolver;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SlottedHotbarTool implements ToolType {

    private static final Map<String, SlottedHotbarTool> REGISTRY = new HashMap<>();
    private static final NamespacedKey KEY = NamespacedKey.fromString("abba:slotted_hotbar_item");

    private final String identifier;
    private final ItemResolver resolver;
    private final int defaultSlot;

    private final boolean isInfinite;

    public SlottedHotbarTool(String name, ItemResolver resolver, int slot) {
        this(name, resolver, slot, false);
    }

    public SlottedHotbarTool(String name, ItemResolver resolver, int slot, boolean isInfinite) {
        this.identifier = name;
        this.resolver = resolver;
        this.defaultSlot = slot;
        this.isInfinite = isInfinite;
        REGISTRY.put(name, this);
    }

    @Nullable
    public static SlottedHotbarTool getStored(String identifier) {
        return REGISTRY.get(identifier);
    }

    @Nullable
    public static SlottedHotbarTool getStored(ItemStack slotItem) {
        if (slotItem == null) {
            return null;
        }

        ItemMeta meta = slotItem.getItemMeta();
        if (meta == null) {
            return null;
        }
        String identifier = meta.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        return getStored(identifier);
    }

    @Override
    public void apply(GamePlayer player) {
        Integer slot = player.hotbarLayout().get(this);
        if (slot == null) {
            slot = this.defaultSlot;
        }

        ItemStack itemStack = this.resolver.get(player);
        itemStack.editMeta((meta) -> {
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, this.identifier);
        });

        player.player().getInventory().setItem(slot, itemStack);
    }

    @Override
    public ItemStack getIcon() {
        return this.resolver.get(null);
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isInfinite() {
        return this.isInfinite;
    }
}

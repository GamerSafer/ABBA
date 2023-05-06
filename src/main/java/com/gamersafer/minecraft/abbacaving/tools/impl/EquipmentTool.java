package com.gamersafer.minecraft.abbacaving.tools.impl;

import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.tools.impl.resolver.ItemResolver;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EquipmentTool implements ToolType {

    private final ItemResolver resolver;
    private final EquipmentSlot slot;

    public EquipmentTool(final ItemResolver resolver, final EquipmentSlot slot) {
        this.resolver = resolver;
        this.slot = slot;
    }

    @Override
    public void apply(final GamePlayer player) {
        player.player().getInventory().setItem(this.slot, this.resolver.get(player));
    }

    @Override
    public ItemStack icon() {
        return this.resolver.get(null);
    }

}

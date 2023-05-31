package com.gamersafer.minecraft.abbacaving.tools;

import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ToolManager {

    private static final int[] SLOTS = {
            0,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            40
    };

    private ToolManager() {

    }

    public static void apply(final GamePlayer player) {
        for (final ToolSpecies toolSpecies : ToolSpecies.values()) {
            for (final ToolType type : toolSpecies.types()) {
                type.apply(player);
            }
        }
    }

    public static Map<SlottedHotbarTool, Integer> serializeHotbarTools(final Player player) {
        final Map<SlottedHotbarTool, Integer> serialized = new HashMap<>();

        for (int slot : SLOTS) {
            final ItemStack slotItem = player.getInventory().getItem(slot);
            final SlottedHotbarTool tool = SlottedHotbarTool.stored(slotItem);
            if (tool != null) {
                serialized.put(tool, slot);
            }

        }

        return serialized;
    }

}

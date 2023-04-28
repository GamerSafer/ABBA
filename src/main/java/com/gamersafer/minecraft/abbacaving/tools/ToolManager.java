package com.gamersafer.minecraft.abbacaving.tools;

import com.gamersafer.minecraft.abbacaving.commands.ToolSpecies;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

public final class ToolManager {

    private ToolManager() {

    }

    public static void apply(final GamePlayer player) {
        for (final ToolSpecies toolSpecies : ToolSpecies.values()) {
            for (final ToolType type : toolSpecies.types()) {
                type.apply(player);
            }
        }
    }

    public static Map<SlottedHotbarTool, Integer> serializeHotbarTools(final GamePlayer player) {
        final Map<SlottedHotbarTool, Integer> serialized = new HashMap<>();

        for (int i = 0; i <= 8; i++) {
            final ItemStack slotItem = player.player().getInventory().getItem(i);
            final SlottedHotbarTool tool = SlottedHotbarTool.stored(slotItem);
            if (tool != null) {
                serialized.put(tool, i);
            }

        }

        return serialized;
    }

}

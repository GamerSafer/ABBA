package com.gamersafer.minecraft.abbacaving.tools;

import com.gamersafer.minecraft.abbacaving.commands.ToolSpecies;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ToolManager {

    public static void apply(GamePlayer player) {
        for (ToolSpecies toolSpecies : ToolSpecies.values()) {
            for (ToolType type : toolSpecies.getTypes()) {
                type.apply(player);
            }
        }
    }

    public static Map<SlottedHotbarTool, Integer> serializeHotbarTools(GamePlayer player) {
        Map<SlottedHotbarTool, Integer> serialized = new HashMap<>();

        for (int i = 0; i <= 8; i++) {
            ItemStack slotItem = player.player().getInventory().getItem(i);
            SlottedHotbarTool tool = SlottedHotbarTool.getStored(slotItem);
            if (tool != null) {
                serialized.put(tool, i);
            }

        }

        return serialized;
    }
}

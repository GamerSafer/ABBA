package com.gamersafer.minecraft.abbacaving.tools;

import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import org.bukkit.inventory.ItemStack;

public interface ToolType {

    void apply(GamePlayer player);

    ItemStack icon();

}

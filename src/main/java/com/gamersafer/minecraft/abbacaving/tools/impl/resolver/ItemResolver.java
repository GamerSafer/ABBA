package com.gamersafer.minecraft.abbacaving.tools.impl.resolver;

import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemResolver {

    @Nullable
    ItemStack get(@Nullable GamePlayer player);

}

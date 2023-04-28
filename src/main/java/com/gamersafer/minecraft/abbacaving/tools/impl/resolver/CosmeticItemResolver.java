package com.gamersafer.minecraft.abbacaving.tools.impl.resolver;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.tools.CosmeticRegistry;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.tools.ToolTypes;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CosmeticItemResolver implements ItemResolver {

    private static final AbbaCavingPlugin INSTANCE = AbbaCavingPlugin.getPlugin(AbbaCavingPlugin.class);

    private final String identifier;

    public CosmeticItemResolver(final String identifier) {
        this.identifier = identifier;
    }

    @Override
    public @Nullable ItemStack get(final @Nullable GamePlayer player) {
        if (player == null) {
            return null;
        }

        final ToolType toolType = ToolTypes.fromIdentifier(this.identifier);
        final CosmeticRegistry.Cosmetic cosmetic = player.selectedCosmetic(toolType);

        if (cosmetic == null) {
            return null;
        }

        return cosmetic.itemStack();
    }

}

package com.gamersafer.minecraft.abbacaving.tools.impl.resolver;

import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DefaultedItemResolver implements ItemResolver {

    private final ItemResolver resolver;
    private final ItemStack defaultItem;

    public DefaultedItemResolver(ItemResolver resolver, ItemStack defaultItem) {
        this.resolver = resolver;
        this.defaultItem = defaultItem;
    }

    @Override
    public @Nullable ItemStack get(@Nullable GamePlayer player) {
        ItemStack fetched = this.resolver.get(player);
        if (fetched == null) {
            return this.defaultItem;
        }

        return fetched;
    }
}

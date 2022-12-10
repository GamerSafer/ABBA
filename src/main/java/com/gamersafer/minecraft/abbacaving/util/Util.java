package com.gamersafer.minecraft.abbacaving.util;

import java.text.DecimalFormat;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class Util {

    private static final DecimalFormat commaFormat = new DecimalFormat("#,###");

    private Util() {
    }

    public static String addCommas(final Object obj) {
        return commaFormat.format(obj);
    }

    public static ItemStack displayName(final ItemStack item, final String displayName) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.displayName(MiniMessage.miniMessage().deserialize(displayName));
        item.setItemMeta(meta);
        return item;
    }

}

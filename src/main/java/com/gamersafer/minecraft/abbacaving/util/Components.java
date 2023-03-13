package com.gamersafer.minecraft.abbacaving.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

public class Components {

    public static Component plainText(String text) {
        return Component.text(text, Style.style(TextDecoration.ITALIC.withState(TextDecoration.State.FALSE)));
    }
}

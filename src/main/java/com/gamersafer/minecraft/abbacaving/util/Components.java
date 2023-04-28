package com.gamersafer.minecraft.abbacaving.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class Components {

    private Components() {

    }

    public static Component plainText(final String text) {
        return Component.text(text, Style.style(TextDecoration.ITALIC.withState(TextDecoration.State.FALSE)));
    }

    public static Component plainText(final String text, final TextColor color) {
        return Component.text(text, Style.style(TextDecoration.ITALIC.withState(TextDecoration.State.FALSE), color));
    }

}

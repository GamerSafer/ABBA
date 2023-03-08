package com.gamersafer.minecraft.abbacaving.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;

final public class Sounds {

    private Sounds() {

    }

    private static final Sound WRITE_TEXT = Sound.sound(org.bukkit.Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER, Sound.Source.PLAYER, 1, 2);
    private static final Sound CHOOSE = Sound.sound(org.bukkit.Sound.BLOCK_STONE_BUTTON_CLICK_OFF, Sound.Source.PLAYER, 1, 2);
    private static final Sound ERROR = Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, Sound.Source.PLAYER, 1, 2);
    private static final Sound PLING = Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, Sound.Source.PLAYER, 1, 2);

    public static void writeText(final Audience audience) {
        audience.playSound(WRITE_TEXT, Sound.Emitter.self());
    }

    public static void choose(final Audience audience) {
        audience.playSound(CHOOSE, Sound.Emitter.self());
    }

    public static void error(final Audience audience) {
        audience.playSound(ERROR, Sound.Emitter.self());
    }

    public static void pling(final Audience audience) {
        audience.playSound(PLING, Sound.Emitter.self());
    }

}
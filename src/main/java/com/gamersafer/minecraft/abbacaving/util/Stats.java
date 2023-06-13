package com.gamersafer.minecraft.abbacaving.util;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.player.GameStats;
import org.bukkit.entity.Player;

import java.util.Map;

public class Stats {

    private static final AbbaCavingPlugin PLUGIN = AbbaCavingPlugin.getPlugin(AbbaCavingPlugin.class);

    public static void dumpGameStats(Player player, Game game, GameStats stats) {
        Messages.message(player, "");
        Messages.message(player, PLUGIN.configMessage("stats-in-game"));
        Messages.message(player, PLUGIN.configMessage("stats-in-game-map"), Map.of("map", game.getMap().getName()));
        Messages.message(player, PLUGIN.configMessage("stats-in-game-score"), Map.of("score", Integer.toString(stats.score())));
        Messages.message(player, PLUGIN.configMessage("stats-in-game-ores"), Map.of("ores", Integer.toString(stats.currentOresMined())));
    }
}

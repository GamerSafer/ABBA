package com.gamersafer.minecraft.abbacaving.util;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.player.GameStats;
import org.bukkit.entity.Player;

import java.util.Map;

public class Stats {

    private static final AbbaCavingPlugin PLUGIN = AbbaCavingPlugin.getPlugin(AbbaCavingPlugin.class);

    public static void dumpGameStats(GamePlayer gamePlayer) {

        Player player = gamePlayer.player();
        GameStats stats = gamePlayer.gameStats();
        if (stats != null) {
            Game game = gamePlayer.gameStats().game();

            Messages.message(player, "");
            Messages.message(player, PLUGIN.configMessage("stats-in-game"));
            Messages.message(player, PLUGIN.configMessage("stats-in-game-map"), Map.of("map", game.mapName()));
            Messages.message(player, PLUGIN.configMessage("stats-in-game-score"), Map.of("score", Integer.toString(gamePlayer.gameStats().score())));
            Messages.message(player, PLUGIN.configMessage("stats-in-game-ores"), Map.of("ores", Integer.toString(gamePlayer.gameStats().currentOresMined())));
        }
    }
}

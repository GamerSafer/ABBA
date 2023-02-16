package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final AbbaCavingPlugin plugin;

    public StatsCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final Player player = (Player) sender;

        final GamePlayer gamePlayer;

        if (args.length == 1) {
            final Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                this.plugin.message(sender, this.plugin.configMessage("not-online"), Map.of("player", args[0]));
                return false;
            }

            gamePlayer = this.plugin.gameTracker().gamePlayer(target);
            this.plugin.message(player, this.plugin.configMessage("stats-other"), Map.of("player", target.getName().toUpperCase()));
        } else {
            gamePlayer = this.plugin.gameTracker().gamePlayer(player);
            this.plugin.message(player, this.plugin.configMessage("stats-own"));
        }

        this.plugin.message(player, "");
        this.plugin.message(player, this.plugin.configMessage("stats-all-time"));
        this.plugin.message(player, this.plugin.configMessage("stats-wins"), Map.of("wins", Util.addCommas(gamePlayer.wins())));
        this.plugin.message(player, this.plugin.configMessage("stats-score"), Map.of("score", Util.addCommas(gamePlayer.highestScore())));
        this.plugin.message(player, this.plugin.configMessage("stats-ores"), Map.of("ores", Util.addCommas(gamePlayer.totalOresMined())));

        if (gamePlayer.gameStats() == null) {
            return true;
        }

        final Game game = gamePlayer.gameStats().game();

        if (game.player(gamePlayer.player()) != null) {
            this.plugin.message(player, "");
            this.plugin.message(player, this.plugin.configMessage("stats-in-game"));
            this.plugin.message(player, this.plugin.configMessage("stats-in-game-map"), Map.of("map", game.mapName()));
            this.plugin.message(player, this.plugin.configMessage("stats-in-game-score"), Map.of("score", Integer.toString(gamePlayer.gameStats().score())));
            this.plugin.message(player, this.plugin.configMessage("stats-in-game-ores"), Map.of("ores", Integer.toString(gamePlayer.gameStats().currentOresMined())));
        } else {
            this.plugin.message(player, "");
            this.plugin.message(player, this.plugin.configMessage("stats-not-in-game"));
        }

        return true;
    }

    private List<String> playerNames() {
        final ArrayList<String> names = new ArrayList<>();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }

        return names;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1)
            return StringUtil.copyPartialMatches(args[0], this.playerNames(), new ArrayList<>());
        else
            return Collections.emptyList();
    }

}

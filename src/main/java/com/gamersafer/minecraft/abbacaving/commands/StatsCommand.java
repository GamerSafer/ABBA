package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import com.gamersafer.minecraft.abbacaving.player.PlayerData;
import com.gamersafer.minecraft.abbacaving.util.Messages;
import com.gamersafer.minecraft.abbacaving.util.Stats;
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
                Messages.message(sender, this.plugin.configMessage("not-online"), Map.of("player", args[0]));
                return false;
            }

            gamePlayer = this.plugin.gameTracker().gamePlayer(target);
            Messages.message(player, this.plugin.configMessage("stats-other"), Map.of("player", target.getName().toUpperCase()));
        } else {
            gamePlayer = this.plugin.gameTracker().gamePlayer(player);
            Messages.message(player, this.plugin.configMessage("stats-own"));
        }

        PlayerData data = gamePlayer.data();
        Messages.message(player, "");
        Messages.message(player, this.plugin.configMessage("stats-all-time"));
        Messages.message(player, this.plugin.configMessage("stats-wins"), Map.of("wins", Util.addCommas(data.wins())));
        Messages.message(player, this.plugin.configMessage("stats-score"), Map.of("score", Util.addCommas(data.highestScore())));
        Messages.message(player, this.plugin.configMessage("stats-ores"), Map.of("ores", Util.addCommas(data.totalOresMined())));

        if (gamePlayer.gameStats() == null) {
            return true;
        }


        Stats.dumpGameStats(gamePlayer);

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

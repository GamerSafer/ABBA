package com.github.colebennett.abbacaving.commands;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.GamePlayer;
import com.github.colebennett.abbacaving.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatsCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public StatsCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final Player player = (Player) sender;

        final GamePlayer gp;
        if (args.length == 1) {
            final Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                this.plugin.message(sender, "<red>Player not online: " + args[0]);
                return false;
            }

            gp = this.plugin.currentGame().player(target);
            this.plugin.message(player, "<dark_aqua><bold>" + target.getName().toUpperCase() + "'s STATS:");
        } else {
            gp = this.plugin.currentGame().player(player);
            this.plugin.message(player, "<dark_aqua><bold>YOUR STATS:");
        }

        this.plugin.message(player, " <gray>Wins: <green>" + Util.addCommas(gp.wins()));
        this.plugin.message(player, " <gray>Highest Score: <green>" + Util.addCommas(gp.highestScore()));
        this.plugin.message(player, " <gray>Ores Mined: <green>" + Util.addCommas(gp.totalOresMined()));
        return true;
    }

}

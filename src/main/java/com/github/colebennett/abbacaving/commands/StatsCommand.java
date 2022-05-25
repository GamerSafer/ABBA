package com.github.colebennett.abbacaving.commands;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.game.GamePlayer;
import com.github.colebennett.abbacaving.util.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatsCommand implements CommandExecutor {

    private final AbbaCavingPlugin plugin;

    public StatsCommand(AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;

        GamePlayer gp;
        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not online: " + args[0]);
                return false;
            }

            gp = plugin.getGame().getPlayer(target);
            plugin.message(player, "<dark_aqua><bold>" + target.getName().toUpperCase() + "'s STATS:");
        } else {
            gp = plugin.getGame().getPlayer(player);
            plugin.message(player, "<dark_aqua><bold>YOUR STATS:");
        }

        plugin.message(player, " <gray>Wins: <green>" + Util.addCommas(gp.getWins()));
        plugin.message(player, " <gray>Highest Score: <green>" + Util.addCommas(gp.getHighestScore()));
        plugin.message(player, " <gray>Ores Mined: <green>" + Util.addCommas(gp.getTotalOresMined()));
        return true;
    }
}

package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class RespawnCountCommand implements CommandExecutor, TabCompleter {

    private final AbbaCavingPlugin plugin;

    public RespawnCountCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final GamePlayer gamePlayer;

        if (args.length >= 1) {
            final Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                this.plugin.message(sender, this.plugin.configMessage("not-online"), Map.of("player", args[0]));
                return false;
            }

            gamePlayer = this.plugin.gameTracker().gamePlayer(target);
        } else {
            if (sender instanceof Player player) {
                gamePlayer = this.plugin.gameTracker().gamePlayer(player);
            } else {
                sender.sendMessage(Component.text("No target player set."));
                return true;
            }
        }

        if (args.length >= 2) {
            final int amount;
            if (args[1].equals("add")) {
                amount = gamePlayer.respawns() + Integer.parseInt(args[2]);
            } else {
                amount = Integer.parseInt(args[1]);
            }

            gamePlayer.respawns(amount);
            this.plugin.message(sender, this.plugin.configMessage("player-respawns-set"), Map.of("amount", Util.addCommas(gamePlayer.respawns())));
            this.plugin.playerDataSource().savePlayerRespawns(gamePlayer);
        } else {
            this.plugin.message(sender, this.plugin.configMessage("player-respawns"), Map.of("amount", Util.addCommas(gamePlayer.respawns())));
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

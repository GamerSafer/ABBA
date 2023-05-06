package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import com.gamersafer.minecraft.abbacaving.lobby.QueueState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.gamersafer.minecraft.abbacaving.util.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class JoinCommand implements CommandExecutor, TabCompleter {

    private final AbbaCavingPlugin plugin;

    public JoinCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final LobbyQueue queue;

        if (args.length == 0 || args[0].equalsIgnoreCase("random")) {
            final LobbyQueue lobbyQueue = this.plugin.lobby().pickFirstQueue();

            if (lobbyQueue == null) {
                this.plugin.message(sender, this.plugin.configMessage("no-open-queues"));
                return true;
            }

            queue = lobbyQueue;
        } else {
            final String input = args[0];

            if (this.plugin.mapSettings(input) == null) {
                this.plugin.message(sender, this.plugin.configMessage("join-invalid-map"));
                return false;
            }

            queue = this.plugin.lobby().lobbyQueue(input);
        }

        final Player player;

        if (args.length == 2) {
            player = Bukkit.getPlayer(args[1]);

            if (player == null) {
                this.plugin.message(sender, this.plugin.configMessage("not-online"), Map.of("player", args[1]));
                return true;
            }
        } else {
            if (sender instanceof Player playerSender) {
                player = playerSender;
            } else {
                return false;
            }
        }

        if (queue == null) {
            this.plugin.message(sender, this.plugin.configMessage("join-invalid-map"));
            return false;
        }

        if (queue.state() == QueueState.LOCKED) {
            this.plugin.message(sender, this.plugin.configMessage("join-running-map"));
            return false;
        }

        if (!queue.acceptingNewPlayers() && !player.hasPermission("abbacaving.join.full")) {
            this.plugin.message(sender, this.plugin.configMessage("join-full"));
            return false;
        }

        final LobbyQueue oldQueue = this.plugin.lobby().lobbyQueue(player);

        if (oldQueue != null) {
            oldQueue.removePlayer(player.getUniqueId());
        }

        this.plugin.lobby().join(queue, player);
        Sounds.pling(player);

        this.plugin.message(sender, this.plugin.configMessage("join-lobby"), Map.of("map", queue.mapName()));

        return true;
    }

    private List<String> mapNames() {
        final List<String> names = new ArrayList<>();

        for (final LobbyQueue queue : this.plugin.lobby().activeQueues()) {
            if (queue.acceptingNewPlayers()) {
                names.add(queue.mapName());
            }
        }

        return names;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1)
            return StringUtil.copyPartialMatches(args[0], this.mapNames(), new ArrayList<>());
        else
            return Collections.emptyList();
    }

}

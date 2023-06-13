package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.map.GameMap;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class ForceStartCommand implements CommandExecutor, TabCompleter {

    private final AbbaCavingPlugin plugin;

    public ForceStartCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        String mapName = null;

        if (args.length > 0) {
            final String input = args[0];

            GameMap map = this.plugin.getMapPool().getMap(input);
            if (map != null) {
                LobbyQueue lobbyQueue = this.plugin.lobby().lobbyQueue(map);
                lobbyQueue.forceStart();
            }
        } else {
            return true;
        }

        return true;
    }

    private List<String> mapNames() {
        final List<String> names = new ArrayList<>();

        for (final LobbyQueue queue : this.plugin.lobby().activeQueues()) {
            if (queue.acceptingNewPlayers()) {
                names.add(queue.getMap().getName());
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

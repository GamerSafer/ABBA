package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.player.GamePlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.gamersafer.minecraft.abbacaving.player.GameStats;
import com.gamersafer.minecraft.abbacaving.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.feature.pagination.Pagination;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ACLookupCommand implements CommandExecutor, TabCompleter, Pagination.Renderer.RowRenderer<GamePlayer> {

    private final Pagination.Builder pagination = Pagination.builder()
            .width(45)
            .renderer(new Pagination.Renderer() {
                @Override
                public Component renderEmpty() {
                    return Component.text("There are no stats for that round!");
                }

                @Override
                public Component renderUnknownPage(final int page, final int pages) {
                    return Component.text("Unknown page selected. " + pages + " total pages.");
                }
            })
            .resultsPerPage(10);

    private final AbbaCavingPlugin plugin;

    public ACLookupCommand(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (args.length < 1) {
            return false;
        }

        final Game game = this.plugin.gameTracker().gameByMapName(args[0]);

        if (game == null) {
            Messages.message(sender, this.plugin.configMessage("no-scores"));
            return true;
        }

        final List<GamePlayer> sortedScores = new ArrayList<>(game.leaderboard().keySet());

        if (sortedScores.isEmpty()) {
            Messages.message(sender, this.plugin.configMessage("no-scores"));
            return true;
        }

        final var pages = this.pagination.build(Component.text(this.plugin.configMessage("lookup-title")).append(Component.text(args[0])), this,
                page -> "/aclookup " + game.gameId() + " " + page);

        int page = 1;

        if (args.length > 1) {
            page = Integer.parseInt(args[1]);
        }

        final var renderedRows = pages.render(sortedScores, page);

        for (final Component row : renderedRows) {
            sender.sendMessage(row);
        }

        return true;
    }

    @Override
    public @NotNull Collection<Component> renderRow(final @Nullable GamePlayer value, final int index) {
        Game game = this.plugin.gameTracker().findGame(value);
        GameStats stats = game.getGameData(value);
        return List.of(Component.text().append(value.player().displayName()).append(Component.text(" - ", NamedTextColor.WHITE))
                .append(Component.text(stats.score())).build());
    }

    private List<String> mapNames() {
        final List<String> names = new ArrayList<>();

        for (final Game game : this.plugin.gameTracker().currentGames()) {
            names.add(game.getMap().getName());
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

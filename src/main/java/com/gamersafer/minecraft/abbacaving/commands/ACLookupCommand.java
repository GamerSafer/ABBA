package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.feature.pagination.Pagination;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ACLookupCommand implements CommandExecutor, Pagination.Renderer.RowRenderer<GamePlayer> {

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

        final Game game = this.plugin.gameTracker().gameById(args[0]);
        final List<GamePlayer> sortedScores = new ArrayList<>(game.leaderboard().keySet());

        if (sortedScores.isEmpty()) {
            this.plugin.message(sender, this.plugin.configMessage("no-scores"));
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
        return List.of(Component.text().append(value.player().displayName()).append(Component.text(" - ", NamedTextColor.WHITE))
                .append(Component.text(value.score())).build());
    }

}

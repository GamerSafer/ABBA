package com.gamersafer.minecraft.abbacaving.player;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.lobby.LobbyQueue;
import com.gamersafer.minecraft.abbacaving.tools.CosmeticRegistry;
import com.gamersafer.minecraft.abbacaving.tools.ToolManager;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class GamePlayer {

    private final AbbaCavingPlugin plugin;
    private final UUID playerUUID;

    @NotNull
    private final PlayerData playerData;
    @Nullable
    private LobbyQueue lobbyQueue;
    @Nullable
    private Game game;

    public GamePlayer(final AbbaCavingPlugin plugin, final UUID playerUUID, PlayerData playerData) {
        this.plugin = plugin;
        this.playerUUID = playerUUID;
        this.playerData = playerData;
    }

    public UUID playerUUID() {
        return this.playerUUID;
    }

    public Player player() {
        return Bukkit.getPlayer(this.playerUUID);
    }

    public PlayerData data() {
        return playerData;
    }


    @Nullable
    public LobbyQueue queue() {
        return this.lobbyQueue;
    }

    public void queue(@Nullable LobbyQueue lobbyQueue) {
        this.lobbyQueue = lobbyQueue;
    }

    @Nullable
    public Game game() {
        return this.game;
    }

    public void game(@Nullable Game game) {
        this.game = game;
    }

}

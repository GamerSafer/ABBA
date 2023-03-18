package com.gamersafer.minecraft.abbacaving.game;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.tools.CosmeticRegistry;
import com.gamersafer.minecraft.abbacaving.tools.ToolManager;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import com.gamersafer.minecraft.abbacaving.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GamePlayer {

    private final AbbaCavingPlugin plugin;
    private final UUID playerUUID;

    private int wins;
    private int highestScore;
    private int totalOresMined;

    private int respawns;

    private GameStats gameStats = null;

    private Map<SlottedHotbarTool, Integer> hotbarLayout = new HashMap<>();
    private final Map<ToolType, CosmeticRegistry.Cosmetic> selectedCosmetics = new HashMap<>();

    public GamePlayer(final AbbaCavingPlugin plugin, final UUID playerUUID) {
        this.plugin = plugin;
        this.playerUUID = playerUUID;
    }

    public UUID playerUUID() {
        return this.playerUUID;
    }

    public Player player() {
        return Bukkit.getPlayer(this.playerUUID);
    }

    public void gameStats(final Game game, final Location spawnLocation) {
        this.gameStats = new GameStats(this, this.plugin, game, spawnLocation);
    }

    public GameStats gameStats() {
        return this.gameStats;
    }

    public CosmeticRegistry.Cosmetic getSelectedCosmetic(final ToolType cosmetic) {
        return this.selectedCosmetics.get(cosmetic);
    }

    public void addSelectedCosmetic(final ToolType toolType, final CosmeticRegistry.Cosmetic cosmetic) {
        this.selectedCosmetics.put(toolType, cosmetic);
    }

    public CosmeticRegistry.Cosmetic removeSelectedCosmetic(final ToolType cosmetic) {
        return this.selectedCosmetics.remove(cosmetic);
    }

    public void removeAllSelectedCosmetics() {
        this.selectedCosmetics.clear();
    }

    public Collection<CosmeticRegistry.Cosmetic> selectedCosmetics() {
        return this.selectedCosmetics.values();
    }

    public Map<SlottedHotbarTool, Integer> hotbarLayout() {
        return this.hotbarLayout;
    }

    public boolean hasCustomHotbarLayout() {
        return this.hotbarLayout != null && !this.hotbarLayout.isEmpty();
    }

    public void hotbarLayout(final Map<SlottedHotbarTool, Integer> hotbarLayout) {
        this.hotbarLayout = hotbarLayout;
    }

    public void hotbarLayout(final SlottedHotbarTool material, final int slot) {
        this.hotbarLayout.put(material, slot);
    }

    public int wins() {
        return this.wins;
    }

    public void wins(final int wins) {
        this.wins = wins;
    }

    public int highestScore() {
        return this.highestScore;
    }

    public void highestScore(final int highestScore) {
        this.highestScore = highestScore;
    }

    public int totalOresMined() {
        return this.totalOresMined;
    }

    public void totalOresMined(final int oresMined) {
        this.totalOresMined = oresMined;
    }

    public int respawns() {
        return this.respawns;
    }

    public void respawns(final int respawns) {
        this.respawns = respawns;
    }

    public void negateRespawn() {
        this.respawns--;
    }

    public void populateHotbar() {
        final Map<SlottedHotbarTool, Integer> hotbarSlots = ToolManager.serializeHotbarTools(this);
        hotbarLayout(hotbarSlots);
    }

    public void saveHotbar() {
        this.plugin.playerDataSource().savePlayerHotbar(this);
    }

    public static final class GameStats {

        private final GamePlayer gamePlayer;
        private final AbbaCavingPlugin plugin;
        private final Game game;

        private Location spawn;
        private Location respawnLocation = null;
        private int score;
        private int currentOresMined;
        private boolean surpassedHighestScore;
        private boolean isDead;
        private boolean hasRespawned;
        private boolean showRespawnGui = false;

        GameStats(final GamePlayer gamePlayer, final AbbaCavingPlugin plugin, final Game game, final Location spawn) {
            this.gamePlayer = gamePlayer;
            this.plugin = plugin;
            this.game = game;
            this.spawn = spawn;
        }

        public Game game() {
            return this.game;
        }

        public int score() {
            return this.score;
        }

        public void score(final int score) {
            this.score = score;
        }

        public int currentOresMined() {
            return this.currentOresMined;
        }

        public void currentOresMined(final int oresMined) {
            this.currentOresMined = oresMined;
        }

        public boolean isDead() {
            return this.isDead;
        }

        public boolean hasRespawned() {
            return this.hasRespawned;
        }

        public Location spawnLocation() {
            return this.spawn;
        }

        public void spawnLocation(final Location spawn) {
            this.spawn = spawn;
        }

        public void isDead(final boolean isDead) {
            this.isDead = isDead;
        }

        public void hasRespawned(final boolean hasRespawned) {
            this.hasRespawned = hasRespawned;
        }

        public void respawnLocation(final Location location) {
            this.respawnLocation = location;
        }

        public Location respawnLocation() {
            return Objects.requireNonNullElse(this.respawnLocation, this.spawn);
        }

        public boolean showRespawnGui() {
            return this.showRespawnGui;
        }

        public void showRespawnGui(final boolean showRespawnGui) {
            this.showRespawnGui = showRespawnGui;
        }

        public void addScore(final int amount, final String rewardName) {
            this.score += amount;

            final float exp = this.gamePlayer.player().getExp() + ((float) amount * .01f);
            this.gamePlayer.player().setExp(exp >= 1f ? 0 : exp);

            this.gamePlayer.player().playSound(this.gamePlayer.player().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            this.plugin.message(this.gamePlayer.player(), this.plugin.configMessage("gained-points"), Map.of(
                    "amount", Integer.toString(amount),
                    "reward", rewardName,
                    "optional-s", amount != 1 ? "s" : ""
            ));

            if (this.score > this.gamePlayer.highestScore) {
                if (!this.surpassedHighestScore) {
                    this.surpassedHighestScore = true;
                    if (this.gamePlayer.highestScore >= this.plugin.getConfig().getInt("min-score-to-broadcast-new-record")) {
                        this.plugin.broadcast(this.plugin.configMessage("new-high-score"), Map.of(
                                "player", this.gamePlayer.player().displayName(),
                                "score", Component.text(Util.addCommas(this.gamePlayer.highestScore))
                        ));
                    }
                }
                this.gamePlayer.highestScore = this.score;
            }
        }

    }

}

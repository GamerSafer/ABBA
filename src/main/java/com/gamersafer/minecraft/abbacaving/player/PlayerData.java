package com.gamersafer.minecraft.abbacaving.player;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.datasource.DataSource;
import com.gamersafer.minecraft.abbacaving.tools.CosmeticRegistry;
import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PlayerData {

    private static final AbbaCavingPlugin PLUGIN = AbbaCavingPlugin.getPlugin(AbbaCavingPlugin.class);

    private final UUID owner;
    private int wins;
    private int highestScore;
    private int totalOresMined;
    private int respawns;
    private Map<SlottedHotbarTool, Integer> hotbarLayout;
    private final Map<ToolType, CosmeticRegistry.Cosmetic> cosmetics;

    public PlayerData(UUID owner) {
        this.owner = owner;
        this.hotbarLayout = new HashMap<>();
        this.cosmetics = new HashMap<>();
    }

    public PlayerData(UUID owner, int wins, int highestScore, int totalOresMined, int respawns,
                      Map<SlottedHotbarTool, Integer> hotbarLayout,
                      Map<ToolType, CosmeticRegistry.Cosmetic> cosmetics) {
        this.owner = owner;
        this.wins = wins;
        this.highestScore = highestScore;
        this.totalOresMined = totalOresMined;
        this.respawns = respawns;
        this.hotbarLayout = hotbarLayout;
        this.cosmetics = cosmetics;
    }

    public UUID getOwner() {
        return owner;
    }

    public void incrementWins() {
        this.wins++;
    }

    public int wins() {
        return wins;
    }

    // Highest score
    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    public int highestScore() {
        return highestScore;
    }
    // -

    // Mined ores
    public void incrementMinedOres() {
        this.totalOresMined++;
    }

    public int totalOresMined() {
        return totalOresMined;
    }
    // -

    // Hotbar layout
    public void setHotbarLayout(Map<SlottedHotbarTool, Integer> hotbarLayout) {
        this.hotbarLayout = hotbarLayout;
    }

    public void hotbarLayout(final SlottedHotbarTool material, final int slot) {
        if (material == null) {
            PLUGIN.getLogger().warning("Ignoring null hotbar item " + material + " in slot " + slot);
        } else {
            this.hotbarLayout.put(material, slot);
        }
    }
    // -

    // Respawns
    public void negateRespawn() {
        this.respawns--;
    }

    public void setRespawns(int respawns) {
        this.respawns = respawns;
    }

    public int respawns() {
        return respawns;
    }
    // -

    public Map<SlottedHotbarTool, Integer> getHotbarLayout() {
        return hotbarLayout;
    }

    // Cosmetics
    public Map<ToolType, CosmeticRegistry.Cosmetic> getCosmetics() {
        return cosmetics;
    }

    @Nullable
    public CosmeticRegistry.Cosmetic removeSelectedCosmetic(ToolType type) {
        return this.cosmetics.remove(type);
    }

    public void addSelectedCosmetic(ToolType type, CosmeticRegistry.Cosmetic cosmetic) {
        this.cosmetics.put(type, cosmetic);
    }
    // -


    // Save methods
    public void saveHotbar() {
        PLUGIN.playerDataSource().savePlayerHotbar(this);
    }

    public void saveRespawns() {
        PLUGIN.playerDataSource().savePlayerRespawns(this);
    }

    public void saveAll() {
        DataSource dataSource = PLUGIN.playerDataSource();
        dataSource.savePlayerStats(this);
        this.saveHotbar();
        this.saveRespawns();
        dataSource.savePlayerCosmetics(this);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PlayerData) obj;
        return this.wins == that.wins &&
                this.highestScore == that.highestScore &&
                this.totalOresMined == that.totalOresMined &&
                this.respawns == that.respawns;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wins, highestScore, totalOresMined, respawns);
    }

    @Override
    public String toString() {
        return "PlayerStats[" +
                "wins=" + wins + ", " +
                "highestScore=" + highestScore + ", " +
                "totalOresMined=" + totalOresMined + ", " +
                "respawns=" + respawns + ']';
    }

}

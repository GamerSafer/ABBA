package com.gamersafer.minecraft.abbacaving.player;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.util.Messages;
import com.gamersafer.minecraft.abbacaving.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.Map;
import java.util.Objects;

public final class GameStats {

    private final GamePlayer gamePlayer;
    private final AbbaCavingPlugin plugin;
    private final Game game;

    private int score;
    private int currentOresMined;
    private boolean surpassedHighestScore;
    private boolean isDead;
    private boolean hasRespawned;
    private boolean showRespawnGui = false;

    public GameStats(final GamePlayer gamePlayer, final AbbaCavingPlugin plugin, final Game game) {
        this.gamePlayer = gamePlayer;
        this.plugin = plugin;
        this.game = game;
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

    public void isDead(final boolean isDead) {
        this.isDead = isDead;
    }

    public void hasRespawned(final boolean hasRespawned) {
        this.hasRespawned = hasRespawned;
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
        Messages.message(this.gamePlayer.player(), this.plugin.configMessage("gained-points"), Map.of(
                "amount", Integer.toString(amount),
                "reward", rewardName,
                "optional-s", amount != 1 ? "s" : ""
        ));

        int highestScore = this.gamePlayer.data().highestScore();

        if (this.score > highestScore) {
            if (!this.surpassedHighestScore) {
                this.surpassedHighestScore = true;
                if (highestScore >= this.plugin.getConfig().getInt("min-score-to-broadcast-new-record")) {
                    Messages.broadcast(this.plugin.configMessage("new-high-score"), Map.of(
                            "player", this.gamePlayer.player().displayName(),
                            "score", Component.text(Util.addCommas(highestScore))
                    ));
                }
            }
            this.gamePlayer.data().setHighestScore(this.score);
        }
    }


    @Override
    public String toString() {
        return "GameStats{" +
                "gamePlayer=" + gamePlayer +
                ", plugin=" + plugin +
                ", game=" + game +
                ", score=" + score +
                ", currentOresMined=" + currentOresMined +
                ", surpassedHighestScore=" + surpassedHighestScore +
                ", isDead=" + isDead +
                ", hasRespawned=" + hasRespawned +
                ", showRespawnGui=" + showRespawnGui +
                '}';
    }
}

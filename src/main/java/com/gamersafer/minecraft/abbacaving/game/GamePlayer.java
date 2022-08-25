package com.gamersafer.minecraft.abbacaving.game;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.util.Util;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class GamePlayer {

    private final AbbaCavingPlugin plugin;
    private final Player player;

    private int wins;
    private int score;
    private int highestScore;
    private int currentOresMined;
    private int totalOresMined;
    private boolean surpassedHighestScore;
    private boolean isDead;
    private boolean hasRespawned;
    private Location spawn;
    private int bucketUses;

    public GamePlayer(final AbbaCavingPlugin plugin, final Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Player player() {
        return this.player;
    }

    public int wins() {
        return this.wins;
    }

    public void wins(final int wins) {
        this.wins = wins;
    }

    public int score() {
        return this.score;
    }

    public void score(final int score) {
        this.score = score;
    }

    public int highestScore() {
        return this.highestScore;
    }

    public void highestScore(final int highestScore) {
        this.highestScore = highestScore;
    }

    public int currentOresMined() {
        return this.currentOresMined;
    }

    public void currentOresMined(final int oresMined) {
        this.currentOresMined = oresMined;
    }

    public int totalOresMined() {
        return this.totalOresMined;
    }

    public void totalOresMined(final int oresMined) {
        this.totalOresMined = oresMined;
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

    public int bucketUses() {
        return this.bucketUses;
    }

    public void bucketUses(final int bucketUses) {
        this.bucketUses = bucketUses;
    }

    public void isDead(final boolean isDead) {
        this.isDead = isDead;
    }

    public void hasRespawned(final boolean hasRespawned) {
        this.hasRespawned = hasRespawned;
    }

    public void addScore(final int amount, final String rewardName) {
        this.score += amount;

        final float exp = this.player.getExp() + ((float) amount * .01f);
        this.player.setExp(exp >= 1f ? 0 : exp);

        this.player.playSound(this.player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        this.plugin.message(this.player, this.plugin.configMessage("gained-points"), Map.of(
                "amount", Integer.toString(amount),
                "reward", rewardName,
                "optional-s", amount != 1 ? "s" : ""
        ));

        if (this.score > this.highestScore) {
            if (!this.surpassedHighestScore) {
                this.surpassedHighestScore = true;
                if (this.highestScore >= this.plugin.getConfig().getInt("game.min-score-to-broadcast-new-record")) {
                    this.plugin.broadcast(this.plugin.configMessage("new-high-score"), Map.of(
                       "player", this.player.displayName(),
                       "score", Component.text(Util.addCommas(GamePlayer.this.highestScore))
                    ));
                }
            }
            this.highestScore = this.score;
        }

        this.plugin.currentGame().updateScore(this);
    }

}

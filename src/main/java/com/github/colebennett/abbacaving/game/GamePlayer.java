package com.github.colebennett.abbacaving.game;

import com.github.colebennett.abbacaving.AbbaCavingPlugin;
import com.github.colebennett.abbacaving.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class GamePlayer  {

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

    public GamePlayer(AbbaCavingPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public int getWins() {
        return wins;
    }

    public int getScore() {
        return score;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public int getCurrentOresMined() {
        return currentOresMined;
    }

    public int getTotalOresMined() {
        return totalOresMined;
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean hasRespawned() {
        return hasRespawned;
    }

    public Location getSpawn() {
        return spawn;
    }

    public int getBucketUses() {
        return bucketUses;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setIsDead(boolean isDead) {
        this.isDead = isDead;
    }

    public void setHasRespawned(boolean hasRespawned) {
        this.hasRespawned = hasRespawned;
    }

    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    public void setCurrentOresMined(int oresMined) {
        this.currentOresMined = oresMined;
    }

    public void setTotalOresMined(int oresMined) {
        this.totalOresMined = oresMined;
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn;
    }

    public void setBucketUses(int bucketUses) {
        this.bucketUses = bucketUses;
    }

    public void addScore(int amount, String rewardName) {
        score += amount;

        float exp = player.getExp() + ((float) amount * .01f);
        player.setExp(exp >= 1f ? 0 : exp);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        plugin.message(player, plugin.getMessage("gained-points"), new HashMap<>() {{
            put("amount", Integer.toString(amount));
            put("reward", rewardName);
            put("optional-s", amount != 1 ? "s" : "");
        }});

        if (score > highestScore) {
            if (!surpassedHighestScore) {
                surpassedHighestScore = true;
                if (highestScore >= plugin.getConfig().getInt("game.min-score-to-broadcast-new-record")) {
                    plugin.broadcast(plugin.getMessage("new-high-score"), new HashMap<>() {{
                        put("player", Component.text(player.getName()));
                        put("score", Component.text(Util.addCommas(highestScore)));
                    }});
                }
            }
            highestScore = score;
        }

        plugin.getGame().updateScore(this);
    }
}

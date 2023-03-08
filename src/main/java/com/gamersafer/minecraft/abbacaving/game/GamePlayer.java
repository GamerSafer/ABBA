package com.gamersafer.minecraft.abbacaving.game;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.util.Util;
import dev.lone.itemsadder.api.CustomStack;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GamePlayer {

    private final AbbaCavingPlugin plugin;
    private final UUID playerUUID;

    private int wins;
    private int highestScore;
    private int totalOresMined;

    private int respawns;

    private GameStats gameStats = null;

    private Map<Integer, String> hotbarLayout = new HashMap<>();
    private final Map<String, String> selectedCosmetics = new HashMap<>();

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

    public boolean hasCosmeticSelected(final String cosmetic) {
        return this.selectedCosmetics.containsKey(cosmetic);
    }

    public void addSelectedCosmetic(final String cosmetic) {
        final String material = this.materialForCosmetic(cosmetic);

        this.addSelectedCosmetic(cosmetic, material);
    }

    public void addSelectedCosmetic(final String cosmetic, final String materialKey) {
        this.selectedCosmetics.put(cosmetic, materialKey);

        // remove other cosmetics with the same materialKey
        this.selectedCosmetics.entrySet().removeIf(entry -> {
            if (entry.getKey().equals(cosmetic)) {
                return false;
            }

            return entry.getValue().equals(materialKey);
        });
    }

    // There's a better way to do this, but this works for now.
    private final List<String> TOOL_ITEMS = List.of("IRON_SWORD", "IRON_SHOVEL", "DIAMOND_PICKAXE");
    private final List<String> ARMOR_ITEMS = List.of("IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS");

    public void removeToolCosmetics() {
        this.selectedCosmetics.keySet().removeIf(this.TOOL_ITEMS::contains);
    }

    public void removeArmorCosmetics() {
        this.selectedCosmetics.keySet().removeIf(this.ARMOR_ITEMS::contains);
    }

    public void removeSelectedCosmetic(final String cosmetic) {
        this.selectedCosmetics.remove(cosmetic);
    }

    public void removeAllSelectedCosmetics() {
        this.selectedCosmetics.clear();
    }

    public Set<String> selectedCosmetics() {
        return Collections.unmodifiableSet(this.selectedCosmetics.keySet());
    }

    private String materialForCosmetic(final String cosmetic) {
        final String weaponMaterial = this.plugin.getConfig().getString("cosmetics.weapon." + cosmetic + ".material");

        if (weaponMaterial != null) {
            return weaponMaterial;
        }

        final String armorMaterial = this.plugin.getConfig().getString("cosmetics.armor." + cosmetic + ".material");

        if (armorMaterial != null) {
            return armorMaterial;
        }

        return null;
    }

    public ItemStack selectedWeaponCosmetic(final String materialKey) {
        final ConfigurationSection cosmetics = this.plugin.getConfig().getConfigurationSection("cosmetics.weapon");

        for (final String key : cosmetics.getKeys(false)) {
            final ConfigurationSection cosmetic = cosmetics.getConfigurationSection(key);
            final String cosmeticMaterial = cosmetic.getString("material");

            if (cosmeticMaterial == null || !cosmeticMaterial.equalsIgnoreCase(materialKey)) {
                continue;
            }

            if (!this.player().hasPermission(cosmetic.getString("permission"))) {
                continue;
            }

            if (this.hasCosmeticSelected(key)) {
                return cosmeticById(key);
            }
        }

        return null;
    }

    public ItemStack selectedArmorCosmetic(final String materialKey) {
        final ConfigurationSection cosmetics = this.plugin.getConfig().getConfigurationSection("cosmetics.armor");

        for (final String key : cosmetics.getKeys(false)) {
            final ConfigurationSection cosmetic = cosmetics.getConfigurationSection(key);
            final String cosmeticMaterial = cosmetic.getString("material");

            if (cosmeticMaterial == null || !cosmeticMaterial.equalsIgnoreCase(materialKey)) {
                continue;
            }

            if (!this.player().hasPermission(cosmetic.getString("permission"))) {
                continue;
            }

            if (this.hasCosmeticSelected(key)) {
                return cosmeticById(key);
            }
        }

        return null;
    }

    public static ItemStack cosmeticById(final String id) {
        final CustomStack customStack = CustomStack.getInstance(id);

        if (customStack != null) {
            return customStack.getItemStack();
        }

        return null;
    }

    public Map<Integer, String> hotbarLayout() {
        return this.hotbarLayout;
    }

    public boolean hasCustomHotbarLayout() {
        return this.hotbarLayout != null && !this.hotbarLayout.isEmpty();
    }

    public void hotbarLayout(final Map<Integer, String> hotbarLayout) {
        this.hotbarLayout = hotbarLayout;
    }

    public void hotbarLayout(final Integer slot, final String material) {
        this.hotbarLayout.put(slot, material);
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

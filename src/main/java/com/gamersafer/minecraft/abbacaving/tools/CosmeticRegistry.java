package com.gamersafer.minecraft.abbacaving.tools;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CosmeticRegistry {

    private final Map<ToolType, List<Cosmetic>> cosmetics = new HashMap<>();
    private final Map<String, Cosmetic> identifierToCosmetic = new HashMap<>();

    public CosmeticRegistry(AbbaCavingPlugin plugin) {
        final ConfigurationSection cosmetics = plugin.getConfig().getConfigurationSection("cosmetics");

        for (final String key : cosmetics.getKeys(false)) {
            final ConfigurationSection cosmetic = cosmetics.getConfigurationSection(key);
            final ToolType type = ToolTypes.fromIdentifier(cosmetic.getString("tool"));

            List<Cosmetic> cosmeticList = this.cosmetics.computeIfAbsent(type, k -> new ArrayList<>());

            String item = cosmetic.getString("item");
            if (item == null) {
                item = key;
            }

            Cosmetic newCosmetic = new Cosmetic(
                    item,
                    cosmetic.getString("permission") == null ? "abbacaving.cosmetic." + key : cosmetic.getString("permission"),
                    key,
                    type
            );
            cosmeticList.add(newCosmetic);
            this.identifierToCosmetic.put(key, newCosmetic);
        }
    }

    public Cosmetic get(String cosmetic) {
        return this.identifierToCosmetic.get(cosmetic);
    }

    public List<Cosmetic> get(ToolType cosmetic) {
        return this.cosmetics.getOrDefault(cosmetic, List.of());
    }

    public record Cosmetic(String item, String permission, String identifier, ToolType toolType) {

        public ItemStack itemStack() {
            CustomStack customStack = CustomStack.getInstance(item);
            if (customStack == null) {
                Bukkit.getLogger().warning("Could not find custom stack: " + item);
                return new ItemStack(Material.STONE);
            }
            return customStack.getItemStack();
        }


    }


}

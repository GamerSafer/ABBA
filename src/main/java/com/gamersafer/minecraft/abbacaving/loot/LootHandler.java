package com.gamersafer.minecraft.abbacaving.loot;

import com.gamersafer.minecraft.abbacaving.game.CaveLoot;
import com.gamersafer.minecraft.abbacaving.game.CaveOre;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LootHandler {

    private final Set<CaveOre> ores;
    private Set<CaveLoot> loot = new HashSet<>();
    private final Map<Material, CaveOre> oreMap = new HashMap<>();
    private final Map<Material, CaveLoot> lootMap = new HashMap<>();
    
    public LootHandler(Logger logger, FileConfiguration pointsConfig) {
        List<CaveOre> loadedOres = new ArrayList<>();
        for (final Map<?, ?> entry : pointsConfig.getMapList("ores")) {
            final Map<?, ?> value = (Map<?, ?>) entry.get("value");
            try {
                loadedOres.add(new CaveOre(
                        (String) entry.get("name"),
                        (Integer) value.get("exact"),
                        (Integer) value.get("min"),
                        (Integer) value.get("max"),
                        (Double) value.get("probability"),
                        Material.valueOf((String) entry.get("block")),
                        new ItemStack(Material.valueOf((String) entry.get("drop")))
                ));
            } catch (IllegalArgumentException exception) {
                logger.warning("Failed to load cave ore from type: " + entry.get("drop"));
            }
        }
        logger.info("Loaded " + loadedOres.size() + " ore(s)");

        this.ores = loadedOres.stream()
                .sorted((o1, o2) -> Integer.compare(o2.value(), o1.value()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (final Map<?, ?> entry : pointsConfig.getMapList("loot-items")) {
            final Map<?, ?> value = (Map<?, ?>) entry.get("value");
            this.loot.add(new CaveLoot(
                    (String) entry.get("name"),
                    (String) entry.get("article"),
                    (Integer) value.get("exact"),
                    (Integer) value.get("min"),
                    (Integer) value.get("max"),
                    Material.valueOf((String) entry.get("item"))
            ));
        }

        logger.info("Loaded " + this.loot.size() + " loot item(s)");

        // Post Process
        for (CaveOre ore : this.ores) {
            this.oreMap.put(ore.ore(), ore);
        }
        for (CaveLoot loot : this.loot) {
            this.lootMap.put(loot.itemType(), loot);
        }
    }



    public Set<CaveOre> getOres() {
        return ores;
    }

    public Set<CaveLoot> getLoot() {
        return loot;
    }

    @Nullable
    public CaveOre caveOreFromBlock(final Material type) {
        return this.oreMap.get(type);
    }

    @Nullable
    public CaveLoot lootFromItem(final Material type) {
        return this.lootMap.get(type);
    }
}

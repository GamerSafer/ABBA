package com.gamersafer.minecraft.abbacaving.game.validators;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import de.themoep.randomteleport.searcher.RandomSearcher;
import de.themoep.randomteleport.searcher.validators.LocationValidator;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;

public class BlockValidator extends LocationValidator {

    private final List<Material> invalidBlocks = new ArrayList<>();

    public BlockValidator(final AbbaCavingPlugin plugin, final Game game) {
        super("abba_block");

        final List<String> materials = game.mapSetting("random-teleport.invalid-blocks");
        final List<String> invalidMaterials = new ArrayList<>();

        for (final String materialName : materials) {
            final Material material = Material.getMaterial(materialName);

            if (material != null) {
                this.invalidBlocks.add(material);
            } else {
                invalidMaterials.add(materialName);
            }
        }

        if (!invalidMaterials.isEmpty()) {
            plugin.getLogger().warning("Invalid material names for [" + String.join(",", invalidMaterials) + "] in RTP invalid-blocks");
        }
    }

    @Override
    public boolean validate(final RandomSearcher randomSearcher, final Location location) {
        return !this.invalidBlocks.contains(location.getBlock().getType());
    }

}

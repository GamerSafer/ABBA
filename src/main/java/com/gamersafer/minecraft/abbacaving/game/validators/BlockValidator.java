package com.gamersafer.minecraft.abbacaving.game.validators;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import de.themoep.randomteleport.searcher.RandomSearcher;
import de.themoep.randomteleport.searcher.validators.LocationValidator;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockValidator extends LocationValidator {

    private final List<Material> invalidBlocks = new ArrayList<>();
    private final int minY;
    private final int maxY;

    public BlockValidator(final AbbaCavingPlugin plugin, final Game game) {
        super("abba_block");

        this.minY = game.mapSetting("random-teleport.min-y");
        this.maxY = game.mapSetting("random-teleport.max-y");

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
        final Block block = location.getBlock();

        return this.isBlockSafe(block) && this.isHeightOk(block) && this.isBlockSafe(block.getRelative(BlockFace.UP))
                && this.isBlockSafe(block.getRelative(BlockFace.UP, 2)) && this.isBlockSafe(block.getRelative(BlockFace.DOWN));
    }

    private boolean isBlockSafe(final Block block) {
        return block.isPassable() && !block.isLiquid() && !this.invalidBlocks.contains(block.getType());
    }

    private boolean isHeightOk(final Block block) {
        return block.getY() > this.minY && block.getY() < this.maxY;
    }

}

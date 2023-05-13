package com.gamersafer.minecraft.abbacaving.game.validators;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import de.themoep.randomteleport.searcher.RandomSearcher;
import de.themoep.randomteleport.searcher.validators.LocationValidator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

public class AbbaValidator extends LocationValidator {

    private final Set<Material> invalidBlocks = new HashSet<>();

    public AbbaValidator(final AbbaCavingPlugin plugin, final Game game) {
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
        location.setY(randomSearcher.getMaxY());
        Block block = location.getBlock();
        while (block.getY() >= location.getWorld().getMinHeight() &&
                !(
                        block.isSolid() &&
                        this.isBlockSafe(block.getRelative(BlockFace.UP)) &&
                        this.isBlockSafe(block.getRelative(BlockFace.UP, 2))
                )
        ) {
            block = block.getRelative(BlockFace.DOWN);
        }

        final boolean result = block.isSolid() && this.isBlockSafe(block.getRelative(BlockFace.UP)) && this.isBlockSafe(block.getRelative(BlockFace.UP, 2));
        final Location newLoc = block.getRelative(BlockFace.UP).getLocation();
        location.setY(newLoc.getY());
        return result;
    }

    private boolean isBlockSafe(final Block block) {
        return block.isEmpty() || block.isPassable() && !block.isLiquid() && !this.invalidBlocks.contains(block.getType()) && this.isNotWaterLogged(block);
    }

    public boolean isNotWaterLogged(Block block) {
        BlockData data = block.getBlockData();

        if (data instanceof Waterlogged waterlogged) {
            return !waterlogged.isWaterlogged()
                    || data.getMaterial() == Material.SEAGRASS
                    || data.getMaterial() == Material.TALL_SEAGRASS
                    || data.getMaterial() == Material.KELP
                    || data.getMaterial() == Material.KELP_PLANT;
        } else {
            return true;
        }
    }

}

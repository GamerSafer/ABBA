package com.gamersafer.minecraft.abbacaving.game.validators;

import com.gamersafer.minecraft.abbacaving.game.Game;
import de.themoep.randomteleport.searcher.RandomSearcher;
import de.themoep.randomteleport.searcher.validators.LocationValidator;
import java.util.List;
import org.bukkit.Location;

public class BlockValidator extends LocationValidator {

    private final List<String> invalidBlocks;

    public BlockValidator(final Game game) {
        super("block");

        this.invalidBlocks = game.mapSetting("random-teleport.invalid-blocks");
    }

    @Override
    public boolean validate(final RandomSearcher randomSearcher, final Location location) {
        return !this.invalidBlocks.contains(location.getBlock().getType().name());
    }

}

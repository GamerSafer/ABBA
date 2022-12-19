package com.gamersafer.minecraft.abbacaving.util;

import de.themoep.randomteleport.searcher.RandomSearcher;
import de.themoep.randomteleport.searcher.validators.BlockValidator;
import org.bukkit.Location;

public class CaveLocationValidator extends BlockValidator {

    @Override
    public boolean validate(final RandomSearcher searcher, final Location location) {
        if (location.getBlockY() > 75) {
            return false;
        }

        return true;
    }

}

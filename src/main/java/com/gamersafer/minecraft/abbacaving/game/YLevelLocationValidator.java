package com.gamersafer.minecraft.abbacaving.game;

import de.themoep.randomteleport.searcher.RandomSearcher;
import de.themoep.randomteleport.searcher.validators.LocationValidator;
import org.bukkit.Location;

public class YLevelLocationValidator extends LocationValidator {

    private final int minY;
    private final int maxY;

    public YLevelLocationValidator(final Game game) {
        super("height");

        this.minY = game.mapSetting("random-teleport.min-y");
        this.maxY = game.mapSetting("random-teleport.max-y");
    }

    @Override
    public boolean validate(final RandomSearcher randomSearcher, final Location location) {
        return location.getBlockY() > this.minY && location.getBlockY() < this.maxY;
    }

}

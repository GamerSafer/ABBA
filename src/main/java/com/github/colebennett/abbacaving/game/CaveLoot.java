package com.github.colebennett.abbacaving.game;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;

public class CaveLoot {

    private final String name;
    private final String article;
    private final Integer exactValue;
    private final Integer minValue;
    private final Integer maxValue;
    private final Material itemType;

    public CaveLoot(
            final String name,
            final String article,
            final Integer exactValue,
            final Integer minValue,
            final Integer maxValue,
            final Material itemType
    ) {
        this.name = name;
        this.article = article;
        this.exactValue = exactValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.itemType = itemType;
    }

    public String name() {
        return this.name;
    }

    public String article() {
        return this.article;
    }

    public int value() {
        if (this.exactValue != null) {
            return this.exactValue;
        }
        if (this.minValue != null && this.maxValue != null) {
            return this.minValue + ThreadLocalRandom.current().nextInt((this.maxValue - this.minValue) + 1);
        }
        return 0;
    }

    public Material itemType() {
        return this.itemType;
    }

}

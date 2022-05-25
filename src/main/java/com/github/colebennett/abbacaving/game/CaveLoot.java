package com.github.colebennett.abbacaving.game;

import org.bukkit.Material;

import java.util.concurrent.ThreadLocalRandom;

public class CaveLoot {

    private final String name, article;
    private final Integer exactValue, minValue, maxValue;
    private final Material itemType;

    public CaveLoot(
        String name,
        String article,
        Integer exactValue,
        Integer minValue,
        Integer maxValue,
        Material itemType
    ) {
        this.name = name;
        this.article = article;
        this.exactValue = exactValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.itemType = itemType;
    }

    public String getName() {
        return name;
    }

    public String getArticle() {
        return article;
    }

    public int getValue() {
        if (exactValue != null) {
            return exactValue;
        }
        if (minValue != null && maxValue != null) {
            return minValue + ThreadLocalRandom.current().nextInt((maxValue - minValue) + 1);
        }
        return 0;
    }

    public Material getItemType() {
        return itemType;
    }
}

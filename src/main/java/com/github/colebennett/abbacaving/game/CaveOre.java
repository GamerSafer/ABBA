package com.github.colebennett.abbacaving.game;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class CaveOre {

    private final String name;
    private final Integer exactValue, minValue, maxValue;
    private final Double probability;
    private final Material ore;
    private final ItemStack drop;

    public CaveOre(
        String name,
        Integer exactValue,
        Integer minValue,
        Integer maxValue,
        Double probability,
        Material ore,
        ItemStack drop
    ) {
        this.name = name;
        this.exactValue = exactValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.probability = probability;
        this.ore = ore;
        this.drop = drop;
    }

    public String getName() {
        return name;
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

    public Material getOre() {
        return ore;
    }

    public ItemStack getDrop() {
        return drop;
    }

    public Double getProbability() {
        return probability;
    }
}

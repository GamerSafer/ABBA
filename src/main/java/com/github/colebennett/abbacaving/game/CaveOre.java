package com.github.colebennett.abbacaving.game;

import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CaveOre {

    private final String name;
    private final Integer exactValue;
    private final Integer minValue;
    private final Integer maxValue;
    private final Double probability;
    private final Material ore;
    private final ItemStack drop;

    public CaveOre(
            final String name,
            final Integer exactValue,
            final Integer minValue,
            final Integer maxValue,
            final Double probability,
            final Material ore,
            final ItemStack drop
    ) {
        this.name = name;
        this.exactValue = exactValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.probability = probability;
        this.ore = ore;
        this.drop = drop;
    }

    public String name() {
        return MiniMessage.miniMessage().stripTags(this.name);
    }

    public Component modernName() {
        return MiniMessage.miniMessage().deserialize(this.name);
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

    public Material ore() {
        return this.ore;
    }

    public ItemStack itemDrop() {
        return this.drop;
    }

    public Double probability() {
        return this.probability;
    }

}

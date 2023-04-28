package com.gamersafer.minecraft.abbacaving.commands;

import com.gamersafer.minecraft.abbacaving.tools.ToolType;
import com.gamersafer.minecraft.abbacaving.tools.ToolTypes;
import com.gamersafer.minecraft.abbacaving.util.Components;
import com.gamersafer.minecraft.abbacaving.util.ItemBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ToolSpecies {

    ARMOR(ToolTypes.HELMET, ToolTypes.CHESTPLATE, ToolTypes.LEGGINGS, ToolTypes.BOOTS, ToolTypes.SHIELD),
    TOOL(ToolTypes.SHOVEL, ToolTypes.SWORD, ToolTypes.PICKAXE, ToolTypes.BUCKET, ToolTypes.BEEF, ToolTypes.BOW),
    BLOCK(ToolTypes.BLOCK);

    private final ToolType[] cosmeticTypes;

    ToolSpecies(final ToolType... cosmeticTypes) {
        this.cosmeticTypes = cosmeticTypes;
    }

    public ToolType[] types() {
        return this.cosmeticTypes;
    }

    public static ItemStack display(final ToolSpecies species) {
        return switch (species) {
            case TOOL ->
                    new ItemBuilder(Material.IRON_PICKAXE).name(Components.plainText("Tools", NamedTextColor.GRAY)).build();
            case ARMOR ->
                    new ItemBuilder(Material.DIAMOND_CHESTPLATE).name(Components.plainText("Armor", NamedTextColor.AQUA)).build();
            case BLOCK ->
                    new ItemBuilder(Material.COBBLESTONE).name(Components.plainText("Block", NamedTextColor.GREEN)).build();
        };
    }

}

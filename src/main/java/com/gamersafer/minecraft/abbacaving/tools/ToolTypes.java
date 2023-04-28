package com.gamersafer.minecraft.abbacaving.tools;

import com.gamersafer.minecraft.abbacaving.tools.impl.EquipmentTool;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import com.gamersafer.minecraft.abbacaving.tools.impl.resolver.CosmeticItemResolver;
import com.gamersafer.minecraft.abbacaving.tools.impl.resolver.DefaultedItemResolver;
import com.gamersafer.minecraft.abbacaving.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public interface ToolTypes {

    interface Keys {
        String PICKAXE = "PICKAXE";
        String SWORD = "SWORD";
        String BOW = "BOW";
        String SHOVEL = "SHOVEL";
        String BEEF = "BEEF";
        String BLOCK = "BLOCK";
        String BUCKET = "BUCKET";
        String TORCH = "TORCH";

        String HELMET = "HELMET";
        String CHESTPLATE = "CHESTPLATE";
        String LEGGINGS = "LEGGINGS";
        String BOOTS = "BOOTS";
        String SHIELD = "SHIELD";
    }

    ToolType PICKAXE = new SlottedHotbarTool(Keys.PICKAXE, new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.PICKAXE),
            new ItemBuilder(Material.DIAMOND_PICKAXE)
                    .unbreakable(true)
                    .enchantment(Enchantment.SILK_TOUCH, 1)
                    .miniMessageName("<green><bold>Starter Pickaxe")
                    .build()

    ), 0);

    ToolType SWORD = new SlottedHotbarTool(Keys.SWORD, new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.SWORD),
            new ItemBuilder(Material.IRON_SWORD)
                    .miniMessageName("<green><bold>Starter Sword")
                    .build()

    ), 1);

    ToolType BOW = new SlottedHotbarTool(Keys.BOW, new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.BOW),
            new ItemBuilder(Material.BOW)
                    .unbreakable(true)
                    .enchantment(Enchantment.ARROW_INFINITE, 1)
                    .miniMessageName("<green><bold>Infinite Bow")
                    .build()

    ), 2);

    ToolType SHOVEL = new SlottedHotbarTool(Keys.SHOVEL, new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.SHOVEL),
            new ItemBuilder(Material.IRON_SHOVEL)
                    .miniMessageName("<green><bold>Starter Shovel")
                    .build()

    ), 3);

    ToolType BEEF = new SlottedHotbarTool(Keys.BEEF, new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.BEEF),
            new ItemBuilder(Material.COOKED_BEEF)
                    .miniMessageName("<green><bold>Infinite Steak Supply")
                    .build()

    ), 4);

    ToolType BLOCK = new SlottedHotbarTool(Keys.BLOCK, new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.BLOCK),
            new ItemBuilder(Material.COBBLESTONE)
                    .miniMessageName("<green><bold>Infinite Cobble")
                    .build()

    ), 5, true);

    ToolType BUCKET = new SlottedHotbarTool(Keys.BUCKET, new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.BUCKET),
            new ItemStack(Material.WATER_BUCKET)

    ), 6);

    ToolType TORCH = new SlottedHotbarTool(Keys.TORCH, new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.TORCH),
            new ItemBuilder(Material.TORCH)
                    .miniMessageName("<green><bold>Infinite Torch")
                    .build()

    ), 7, true);

    ToolType HELMET = new EquipmentTool(new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.HELMET),
            new ItemStack(Material.IRON_HELMET)

    ), EquipmentSlot.HEAD);

    ToolType CHESTPLATE = new EquipmentTool(new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.CHESTPLATE),
            new ItemStack(Material.IRON_CHESTPLATE)
    ), EquipmentSlot.CHEST);

    ToolType LEGGINGS = new EquipmentTool(new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.LEGGINGS),
            new ItemStack(Material.IRON_LEGGINGS)
    ), EquipmentSlot.LEGS);

    ToolType BOOTS = new EquipmentTool(new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.BOOTS),
            new ItemStack(Material.IRON_BOOTS)
    ), EquipmentSlot.FEET);

    ToolType SHIELD = new EquipmentTool(new DefaultedItemResolver(
            new CosmeticItemResolver(Keys.SHIELD),
            new ItemStack(Material.SHIELD)
    ), EquipmentSlot.OFF_HAND);

    static ToolType fromIdentifier(String identifier) {
        return ToolTypeRegistry.fromIdentifier(identifier);
    }

}

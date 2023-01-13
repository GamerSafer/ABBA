package com.gamersafer.minecraft.abbacaving.util;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * This is a chainable builder for {@link ItemStack}s in {@link Bukkit}.
 * <br>
 * Example Usage:<br>
 * {@code ItemStack this.is = new ItemBuilder(Material.LEATHER_HELMET).amount(2).data(4).durability(4).enchantment(Enchantment.ARROW_INFINITE).enchantment(Enchantment.LUCK, 2).name(ChatColor.RED + "the name").lore(ChatColor.GREEN + "line 1").lore(ChatColor.BLUE + "line 2").color(Color.MAROON).build();}
 *
 * @author MiniDigger
 * @version 1.2
 */
@SuppressWarnings("unused")
public class ItemBuilder {

    private final ItemStack is;

    /**
     * Initializes the builder with the given {@link Material}.
     *
     * @param mat the {@link Material} to start the builder from
     * @since 1.0
     */
    public ItemBuilder(final Material mat) {
        this.is = new ItemStack(mat);
    }

    /**
     * Inits the builder with the given {@link ItemStack}.
     *
     * @param is the {@link ItemStack} to start the builder from
     * @since 1.0
     */
    public ItemBuilder(final ItemStack is) {
        this.is = is;
    }

    /**
     * Changes the amount of the {@link ItemStack}.
     *
     * @param amount the new amount to set
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder amount(final int amount) {
        this.is.setAmount(amount);
        return this;
    }

    /**
     * Changes the display name of the {@link ItemStack}.
     *
     * @param name the new display name to set
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder name(final Component name) {
        final ItemMeta meta = this.is.getItemMeta();
        meta.displayName(name);
        this.is.setItemMeta(meta);

        return this;
    }

    /**
     * Adds a new line to the lore of the {@link ItemStack}.
     *
     * @param line the new line to add
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder addLoreLine(final Component line) {
        final ItemMeta meta = this.is.getItemMeta();
        List<Component> lore = meta.lore();

        if (lore == null) {
            lore = new ArrayList<>();
        }

        lore.add(line);
        meta.lore(lore);
        this.is.setItemMeta(meta);

        return this;
    }

    /**
     * Sets all lore lines of the {@link ItemStack}.
     *
     * @param lines the new lines to use
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder lore(final List<Component> lines) {
        final ItemMeta meta = this.is.getItemMeta();

        meta.lore(lines);
        this.is.setItemMeta(meta);

        return this;
    }

    /**
     * Changes the durability of the {@link ItemStack}.
     *
     * @param durability the new durability to set
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder durability(final int durability) {
        if (this.is.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage(durability);

            this.is.setItemMeta(damageable);
        }

        return this;
    }

    /**
     * Changes the data of the {@link ItemStack}.
     *
     * @param data the new data to set
     * @return this builder for chaining
     * @since 1.0
     */
    @SuppressWarnings("deprecation")
    public ItemBuilder data(final int data) {
        this.is.setData(new MaterialData(this.is.getType(), (byte) data));
        return this;
    }

    public ItemBuilder customModelData(final int customModelData) {
        final ItemMeta itemMeta = this.is.getItemMeta();
        itemMeta.setCustomModelData(customModelData);
        this.is.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder itemFlags(final ItemFlag... itemFlags) {
        this.is.addItemFlags(itemFlags);
        return this;
    }

    /**
     * Adds an {@link Enchantment} with the given level to the {@link ItemStack}.
     *
     * @param enchantment the enchantment to add
     * @param level the level of the enchantment
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder enchantment(final Enchantment enchantment, final int level) {
        this.is.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    /**
     * Adds an {@link Enchantment} with the level 1 to the {@link ItemStack}.
     *
     * @param enchantment the enchantment to add
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder enchantment(final Enchantment enchantment) {
        this.is.addUnsafeEnchantment(enchantment, 1);
        return this;
    }

    /**
     * Changes the {@link Material} of the {@link ItemStack}.
     *
     * @param material the new material to set
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder type(final Material material) {
        this.is.setType(material);
        return this;
    }

    /**
     * Clears the lore of the {@link ItemStack}.
     *
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder clearLore() {
        final ItemMeta meta = this.is.getItemMeta();
        meta.lore(new ArrayList<>());
        this.is.setItemMeta(meta);
        return this;
    }

    /**
     * Clears the list of {@link Enchantment}s of the {@link ItemStack}.
     *
     * @return this builder for chaining
     * @since 1.0
     */
    public ItemBuilder clearEnchantments() {
        for (final Enchantment e : this.is.getEnchantments().keySet()) {
            this.is.removeEnchantment(e);
        }
        return this;
    }

    /**
     * Sets the {@link Color} of a part of leather armor.
     *
     * @param color the {@link Color} to use
     * @return this builder for chaining
     * @since 1.1
     */
    public ItemBuilder color(final Color color) {
        if (this.is.getItemMeta() instanceof LeatherArmorMeta leatherArmorMeta) {
            leatherArmorMeta.setColor(color);
            this.is.setItemMeta(leatherArmorMeta);
        } else {
            throw new IllegalArgumentException("color() only applicable for leather armor!");
        }

        return this;
    }

    /**
     * Adds a {@link PotionEffect} to a potion.
     *
     * @param potionEffect the {@link PotionEffect} to use
     * @param overwrite true if any existing effect of the same type should be overwritten
     * @return this builder for chaining
     * @since 1.1
     */
    public ItemBuilder potionEffect(final PotionEffect potionEffect, final boolean overwrite) {
        if (this.is.getItemMeta() instanceof PotionMeta potionMeta) {
            potionMeta.addCustomEffect(potionEffect, overwrite);
            this.is.setItemMeta(potionMeta);
        }

        return this;
    }

    /**
     * Adds a {@link PotionEffect} to a potion.
     *
     * @param potionEffect the {@link PotionEffect} to use
     * @return this builder for chaining
     * @since 1.1
     */
    public ItemBuilder potionEffect(final PotionEffect potionEffect) {
        return this.potionEffect(potionEffect, true);
    }

    public ItemBuilder potionEffect(final PotionEffectType potionEffectType, final int duration, final int amplifier) {
        return this.potionEffect(new PotionEffect(potionEffectType, duration, amplifier));
    }

    /**
     * Builds the {@link ItemStack}.
     *
     * @return the created {@link ItemStack}
     * @since 1.0
     */
    public ItemStack build() {
        return this.is;
    }

}

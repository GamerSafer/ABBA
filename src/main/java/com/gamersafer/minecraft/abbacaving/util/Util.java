package com.gamersafer.minecraft.abbacaving.util;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

// TODO: modernize, replace legacy with components
public final class Util {

    private static final DecimalFormat commaFormat = new DecimalFormat("#,###");

    private Util() {
    }

    public static boolean inBounds(final Location location, final Location bound1, final Location bound2) {
        return Math.min(bound1.getX(), bound2.getX()) <= location.getX() && location.getX() <= Math.max(bound1.getX(), bound2.getX()) &&
                Math.min(bound1.getY(), bound2.getY()) <= location.getY() && location.getY() <= Math.max(bound1.getY(), bound2.getY()) &&
                Math.min(bound1.getZ(), bound2.getZ()) <= location.getZ() && location.getZ() <= Math.max(bound1.getZ(), bound2.getZ());
    }

    public static String addCommas(final Object obj) {
        return commaFormat.format(obj);
    }

    public static ItemStack displayName(final ItemStack item, final String displayName) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.displayName(MiniMessage.miniMessage().deserialize(displayName));
        item.setItemMeta(meta);
        return item;
    }

    public static World loadMap(final File originalMap, final String gameId) {
        final File mapDestination = new File(Bukkit.getWorldContainer(), gameId);

        if (mapDestination.exists()) {
            deleteWorld(mapDestination);
        }

        try {
            FileUtils.copyDirectory(originalMap, mapDestination);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final World world = Bukkit.createWorld(new WorldCreator(gameId));
        world.setKeepSpawnInMemory(false);
        world.setTime(0);
        world.setStorm(false);
        world.setThundering(false);
        world.setAutoSave(false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, Boolean.FALSE);
        return world;
    }

    public static void deleteWorld(final World world) {
        if (Bukkit.getWorld(world.getUID()) != null) {
            Bukkit.unloadWorld(world, false);
        }

        deleteWorld(world.getWorldFolder());
    }

    public static void deleteWorld(final File worldDir) {
        final File[] fs = worldDir.listFiles();
        if (fs == null)
            return;
        for (final File f : fs) {
            if (f.isDirectory()) {
                for (final File ff : f.listFiles()) {
                    ff.delete();
                }
                f.delete();
            } else {
                f.delete();
            }
        }
        worldDir.delete();
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
            final Map<K, V> map, final boolean desc) {
        final List<Entry<K, V>> entries = new LinkedList<>(map.entrySet());

        entries.sort((o1, o2) -> {
            if (desc) {
                return o2.getValue().compareTo(o1.getValue());
            } else {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        final Map<K, V> result = new LinkedHashMap<>(entries.size());

        for (final Entry<K, V> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom rnd = new SecureRandom();

    public static String randomString(final int len) {
        final StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

}

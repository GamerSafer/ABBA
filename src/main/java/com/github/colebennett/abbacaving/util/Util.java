package com.github.colebennett.abbacaving.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;

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

    public static World loadMap(final File archive, final String mapName) {
        final File folder = new File(Bukkit.getWorldContainer(), mapName);
        if (folder.exists()) {
            deleteWorld(folder);
        }

        final TarGZipUnArchiver ua = new TarGZipUnArchiver();
        final ConsoleLoggerManager manager = new ConsoleLoggerManager();
        manager.initialize();
        ua.enableLogging(manager.getLoggerForComponent("bla"));
        ua.setSourceFile(archive);
        folder.mkdirs();
        ua.setDestDirectory(folder);
        ua.extract();

        final World world = Bukkit.createWorld(new WorldCreator(mapName));
        world.setTime(0);
        world.setStorm(false);
        world.setThundering(false);
        world.setAutoSave(false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, Boolean.FALSE);
        return world;
    }

    public static void unzip(final File file, final String newFolderName) throws IOException {
        final String fileName = file.getName();
        final int exindex = fileName.lastIndexOf(".");
        final String dirName = fileName.substring(0, exindex);

        final File toDir = new File(file.getParent(), dirName + "/");
        unzip(file, toDir, newFolderName);
    }

    public static void unzip(final File file, final File toDir, final String newFolderName) throws IOException {
        toDir.mkdirs();
        if (!toDir.exists()) {
            throw new IllegalStateException();
        }
        final ZipFile zipFile = new ZipFile(file);
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();

        int len;
        final byte[] read = new byte[1024];
        String base = null;
        while (entries.hasMoreElements()) {
            final ZipEntry ze = entries.nextElement();
            if (base == null) {
                base = ze.getName().split("/")[0];
            }

            final File outFile = new File(toDir, ze.getName());
            if (ze.isDirectory()) {
                outFile.mkdirs();
            } else {
                BufferedInputStream bis = null;
                BufferedOutputStream bos = null;
                try {
                    final InputStream is = zipFile.getInputStream(ze);
                    bis = new BufferedInputStream(is);
                    bos = new BufferedOutputStream(new FileOutputStream(outFile));
                    while ((len = bis.read(read)) != -1) {
                        bos.write(read, 0, len);
                    }
                } catch (final FileNotFoundException e) {
                    throw e;
                } catch (final IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bis != null) {
                            bis.close();
                        }
                    } catch (final IOException ignored) {
                    }
                    try {
                        if (bos != null) {
                            bos.close();
                        }
                    } catch (final IOException ignored) {
                    }
                }
            }
        }
        if (newFolderName != null) {
            new File(toDir, base).renameTo(new File(toDir, newFolderName));
        }
    }

    public static void deleteWorld(final World world) {
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

}

package com.github.colebennett.abbacaving.util;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Util {

    private Util() {}

    public static boolean inBounds(Location location, Location bound1, Location bound2) {
        return Math.min(bound1.getX(), bound2.getX()) <= location.getX() && location.getX() <= Math.max(bound1.getX(), bound2.getX()) &&
                Math.min(bound1.getY(), bound2.getY()) <= location.getY() && location.getY() <= Math.max(bound1.getY(), bound2.getY()) &&
                Math.min(bound1.getZ(), bound2.getZ()) <= location.getZ() && location.getZ() <= Math.max(bound1.getZ(), bound2.getZ());
    }

    private static final DecimalFormat commaFormat = new DecimalFormat("#,###");

    public static String addCommas(Object obj) {
        return commaFormat.format(obj);
    }

    public static ItemStack setDisplayName(ItemStack item, String displayName) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setDisplayName(colorize(displayName));
        item.setItemMeta(meta);
        return item;
    }

    public static String colorize(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static World loadMap(File archive, String mapName) {
        File folder = new File(Bukkit.getWorldContainer(), mapName);
        if (folder.exists()) {
            deleteWorld(folder);
        }

        TarGZipUnArchiver ua = new TarGZipUnArchiver();
        ConsoleLoggerManager manager = new ConsoleLoggerManager();
        manager.initialize();
        ua.enableLogging(manager.getLoggerForComponent("bla"));
        ua.setSourceFile(archive);
        folder.mkdirs();
        ua.setDestDirectory(folder);
        ua.extract();

        World world = Bukkit.createWorld(new WorldCreator(mapName));
        world.setTime(0);
        world.setStorm(false);
        world.setThundering(false);
        world.setAutoSave(false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, Boolean.FALSE);
        return world;
    }

    public static void unzip(File file, String newFolderName) throws IOException {
        String fileName = file.getName();
        int exindex = fileName.lastIndexOf(".");
        String dirName = fileName.substring(0, exindex);

        File toDir = new File(file.getParent(), dirName + "/");
        unzip(file, toDir, newFolderName);
    }

    public static void unzip(File file, File toDir, String newFolderName) throws IOException {
        toDir.mkdirs();
        if (!toDir.exists()) {
            throw new IllegalStateException();
        }
        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        int len;
        byte[] read = new byte[1024];
        String base = null;
        while (entries.hasMoreElements()) {
            ZipEntry ze = entries.nextElement();
            if (base == null) {
                base = ze.getName().split("/")[0];
            }

            File outFile = new File(toDir, ze.getName());
            if (ze.isDirectory()) {
                outFile.mkdirs();
            } else {
                BufferedInputStream bis = null;
                BufferedOutputStream bos = null;
                try {
                    InputStream is = zipFile.getInputStream(ze);
                    bis = new BufferedInputStream(is);
                    bos = new BufferedOutputStream(new FileOutputStream(outFile));
                    while ((len = bis.read(read)) != -1) {
                        bos.write(read, 0, len);
                    }
                } catch (FileNotFoundException e) {
                    throw e;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bis != null) {
                            bis.close();
                        }
                    } catch (IOException ignored) {
                    }
                    try {
                        if (bos != null) {
                            bos.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        if (newFolderName != null) {
            new File(toDir, base).renameTo(new File(toDir, newFolderName));
        }
    }

    public static void deleteWorld(World world) {
        deleteWorld(world.getWorldFolder());
    }

    public static void deleteWorld(File worldDir) {
        File[] fs = worldDir.listFiles();
        if (fs == null)
            return;
        for (File f : fs) {
            if (f.isDirectory()) {
                for (File ff : f.listFiles()) {
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
            Map<K, V> map, final boolean desc) {
        List<Entry<K, V>> entries = new LinkedList<>(map.entrySet());
        Collections.sort(entries, (o1, o2) -> {
            if (desc) {
                return o2.getValue().compareTo(o1.getValue());
            } else {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        Map<K, V> result = new LinkedHashMap<>(entries.size());
        for (Entry<K, V> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

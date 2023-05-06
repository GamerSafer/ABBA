package com.gamersafer.minecraft.abbacaving.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Messages {

    private static final Logger LOGGER = LoggerFactory.getLogger(Messages.class);

    public static void broadcast(String message) {
        if (message == null) {
            message = "";
        }

        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message));
        LOGGER.info("Broadcast: \"" + message + "\"");
    }

    public static void broadcast(String message, final Map<String, Component> placeholders) {
        if (message == null) {
            message = "";
        }

        final List<TagResolver> resolvers = new ArrayList<>();

        for (final Map.Entry<String, Component> entry : placeholders.entrySet()) {
            resolvers.add(TagResolver.resolver(entry.getKey(), Tag.inserting(entry.getValue())));
        }

        broadcast(message, resolvers.toArray(new TagResolver[]{}));
    }

    public static void broadcast(final String message, final TagResolver... placeholders) {
        Bukkit.getServer().sendMessage(MiniMessage.miniMessage().deserialize(message, placeholders));
    }

    public static void message(final CommandSender sender, String message) {
        if (message == null) {
            message = "";
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public static void message(final CommandSender sender, String message, final Map<String, String> placeholders) {
        if (message == null) {
            message = "";
        }

        final List<TagResolver> resolvers = new ArrayList<>();

        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers.add(TagResolver.resolver(entry.getKey(), Tag.inserting(Component.text(entry.getValue()))));
        }

        message(sender, message, resolvers.toArray(new TagResolver[]{}));
    }

    public static void message(final CommandSender sender, String message, final TagResolver... placeholders) {
        if (message == null) {
            message = "";
        }

        final TagResolver name = TagResolver.resolver("name", Tag.inserting(Component.text(sender.getName())));
        final TagResolver resolvers = TagResolver.resolver(TagResolver.resolver(placeholders), name);

        sender.sendMessage(MiniMessage.miniMessage().deserialize(message, resolvers));
    }

}

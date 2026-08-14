package com.alkacode.rankup.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private TextUtil() {
    }

    public static Component parse(String raw) {
        return MINI_MESSAGE.deserialize(raw == null ? "" : raw);
    }

    /**
     * Remove toda formatacao MiniMessage, deixando so o texto puro - usado onde o
     * consumidor final nao interpreta tags/cores (ex: nome "limpo" de rank via PAPI).
     */
    public static String plainText(String raw) {
        return PLAIN.serialize(parse(raw));
    }

    /**
     * Converte MiniMessage para codigos de cor legacy (secao/paragraph sign) - formato
     * que a maioria dos plugins de scoreboard (ex: LeafScore) espera ao ler um placeholder
     * do PlaceholderAPI, ja que eles nao interpretam tags MiniMessage cruas.
     */
    public static String legacy(String raw) {
        return LEGACY.serialize(parse(raw));
    }

    public static Component parse(String raw, Map<String, String> placeholders) {
        String replaced = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            replaced = replaced.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return MINI_MESSAGE.deserialize(replaced);
    }

    public static List<Component> parseList(List<String> raws, Map<String, String> placeholders) {
        return raws.stream().map(raw -> parse(raw, placeholders)).collect(Collectors.toList());
    }

    public static String plain(String raw, Map<String, String> placeholders) {
        String replaced = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            replaced = replaced.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return replaced;
    }
}

package com.alkacode.rankup.config;

import com.alkacode.rankup.util.TextUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.Map;

public final class Messages {

    private final ConfigManager configManager;

    public Messages(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void send(Audience audience, String path) {
        send(audience, path, Map.of());
    }

    public void send(Audience audience, String path, Map<String, String> placeholders) {
        audience.sendMessage(build(path, placeholders));
    }

    public Component build(String path, Map<String, String> placeholders) {
        String raw = configManager.prefix() + configManager.rawMessage(path);
        return TextUtil.parse(raw, placeholders);
    }
}

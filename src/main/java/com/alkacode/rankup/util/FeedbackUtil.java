package com.alkacode.rankup.util;

import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Som + particula + title/subtitle a partir de uma secao `feedback.<evento>` do config.yml. Cada pedaco e opcional/desativavel sozinho. */
public final class FeedbackUtil {

    private static final Logger LOGGER = Logger.getLogger("AlkaRankUp");

    private FeedbackUtil() {
    }

    public static void play(Player player, ConfigurationSection section, Map<String, String> placeholders) {
        if (section == null) {
            return;
        }
        playSound(player, section);
        playParticle(player, section);
        showTitle(player, section, placeholders);
    }

    private static void playSound(Player player, ConfigurationSection section) {
        String soundName = section.getString("sound", "");
        if (soundName == null || soundName.isBlank()) {
            return;
        }
        try {
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
            if (sound == null) {
                return;
            }
            float volume = (float) section.getDouble("sound-volume", 1.0);
            float pitch = (float) section.getDouble("sound-pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Som invalido em feedback: " + soundName, e);
        }
    }

    private static void playParticle(Player player, ConfigurationSection section) {
        String particleName = section.getString("particle", "");
        if (particleName == null || particleName.isBlank()) {
            return;
        }
        try {
            Particle particle = Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(particleName.toLowerCase()));
            if (particle == null) {
                return;
            }
            int count = section.getInt("particle-count", 20);
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.5, 0.5, 0.5, 0.05);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Particula invalida em feedback: " + particleName, e);
        }
    }

    private static void showTitle(Player player, ConfigurationSection section, Map<String, String> placeholders) {
        String rawTitle = section.getString("title", "");
        String rawSubtitle = section.getString("subtitle", "");
        if ((rawTitle == null || rawTitle.isBlank()) && (rawSubtitle == null || rawSubtitle.isBlank())) {
            return;
        }
        player.showTitle(Title.title(
                TextUtil.parse(rawTitle == null ? "" : rawTitle, placeholders),
                TextUtil.parse(rawSubtitle == null ? "" : rawSubtitle, placeholders)));
    }
}

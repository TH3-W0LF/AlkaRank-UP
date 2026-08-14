package com.alkacode.rankup.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integracao soft com o AlkaVips - hoje so confirma presenca (usado pro sync com
 * LuckPerms ainda por vir, quando o mapeamento rank -> grupo/prefixo for definido).
 * Reflexao pura - mesmo motivo documentado em EconomyHook.
 */
public final class AlkaVipsHook {

    private AlkaVipsHook() {
    }

    public static AlkaVipsHook tryHook(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("AlkaVips") == null) {
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("com.alkacode.vips.api.AlkaVipsAPI");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) {
                return null;
            }
            logger.info("AlkaVips detectado.");
            return new AlkaVipsHook();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "AlkaVips encontrado mas a API nao carregou via reflexao.", t);
            return null;
        }
    }
}

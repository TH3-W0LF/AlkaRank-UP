package com.alkacode.rankup.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integracao com o AlkaEconomy - agora softdepend (era hard antes de 2026-08-10), entao
 * precisa ser resolvida em runtime como qualquer outra integracao soft do ecossistema
 * Alka*, nunca com o plugin desabilitado inteiro so porque o Economy nao carregou ainda.
 *
 * Reflexao pura - NUNCA importar com.alkacode.economy.* direto aqui (mesmo motivo
 * documentado no AlkaMinesHook/AlkaShopHook/AlkaVipsHook: com o dependency agora soft,
 * so ter o tipo em qualquer assinatura desta classe faz a JVM tentar resolver a classe
 * assim que o hook e carregado, mesmo com softdepend no plugin.yml e um check de
 * presenca antes - resultado real ja observado noutros plugins: NoClassDefFoundError
 * ao iniciar sem o AlkaEconomy instalado).
 */
public final class EconomyHook {

    private final Object economyManager;
    private final Method hasMethod;
    private final Method getBalanceMethod;
    private final Method removeBalanceMethod;
    private final Method isValidCurrencyMethod;
    private final Method formatValueMethod;

    private EconomyHook(Object economyManager, Method hasMethod, Method getBalanceMethod,
                         Method removeBalanceMethod, Method isValidCurrencyMethod, Method formatValueMethod) {
        this.economyManager = economyManager;
        this.hasMethod = hasMethod;
        this.getBalanceMethod = getBalanceMethod;
        this.removeBalanceMethod = removeBalanceMethod;
        this.isValidCurrencyMethod = isValidCurrencyMethod;
        this.formatValueMethod = formatValueMethod;
    }

    public static EconomyHook tryHook(Logger logger) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("AlkaEconomy");
        if (plugin == null) {
            return null;
        }
        try {
            Class<?> pluginClass = Class.forName("com.alkacode.economy.AlkaEconomyPlugin");
            if (!pluginClass.isInstance(plugin)) {
                return null;
            }
            Object economyManager = pluginClass.getMethod("getEconomyManager").invoke(plugin);

            Class<?> managerClass = Class.forName("com.alkacode.economy.EconomyManager");
            Method has = managerClass.getMethod("has", UUID.class, String.class, double.class);
            Method getBalance = managerClass.getMethod("getBalance", UUID.class, String.class);
            Method removeBalance = managerClass.getMethod("removeBalance", UUID.class, String.class, double.class);
            Method isValidCurrency = managerClass.getMethod("isValidCurrency", String.class);
            Method formatValue = managerClass.getMethod("formatValue", double.class);

            logger.info("AlkaEconomy detectado - rank-up e prestigio vao cobrar normalmente.");
            return new EconomyHook(economyManager, has, getBalance, removeBalance, isValidCurrency, formatValue);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "AlkaEconomy encontrado mas a API nao carregou via reflexao (versao incompativel?) - "
                    + "rank-up e prestigio ficam indisponiveis.", t);
            return null;
        }
    }

    public boolean has(UUID uuid, String currency, double amount) {
        try {
            return (boolean) hasMethod.invoke(economyManager, uuid, currency, amount);
        } catch (Throwable t) {
            return false;
        }
    }

    public double getBalance(UUID uuid, String currency) {
        try {
            return (double) getBalanceMethod.invoke(economyManager, uuid, currency);
        } catch (Throwable t) {
            return 0.0;
        }
    }

    public void removeBalance(UUID uuid, String currency, double amount) {
        try {
            removeBalanceMethod.invoke(economyManager, uuid, currency, amount);
        } catch (Throwable t) {
            // Silencioso de proposito - o chamador ja checou has() antes de chegar aqui.
        }
    }

    public boolean isValidCurrency(String currency) {
        try {
            return (boolean) isValidCurrencyMethod.invoke(economyManager, currency);
        } catch (Throwable t) {
            return false;
        }
    }

    public String formatValue(double value) {
        try {
            return (String) formatValueMethod.invoke(null, value);
        } catch (Throwable t) {
            return String.valueOf(value);
        }
    }
}

package com.alkacode.rankup.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integracao soft com o AlkaTime - plugin novo, AINDA NAO EXISTE no ecossistema (ver
 * requisito "online_time" nos ranks). Escrito seguindo a mesma convencao de API de
 * todo Alka* (com.alkacode.time.api.AlkaTimeAPI, servico registrado no ServicesManager,
 * metodo assincrono retornando CompletableFuture, mesmo padrao do AlkaVipsAPI) - ate o
 * AlkaTime nascer de verdade, {@link #tryHook} sempre retorna null (plugin nao existe,
 * softdepend simplesmente nunca resolve) e qualquer rank com requisito de tempo online
 * fica bloqueado com uma mensagem clara em vez de quebrar. Quando o AlkaTime for criado,
 * so precisa bater com essa assinatura (ou ajustar aqui) - nada mais muda.
 *
 * Reflexao pura - mesmo motivo documentado em EconomyHook/AlkaVipsHook.
 */
public final class TimeHook {

    private final Object api;
    private final Method getOnlineSecondsMethod;
    private final Logger logger;

    private TimeHook(Object api, Method getOnlineSecondsMethod, Logger logger) {
        this.api = api;
        this.getOnlineSecondsMethod = getOnlineSecondsMethod;
        this.logger = logger;
    }

    public static TimeHook tryHook(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("AlkaTime") == null) {
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("com.alkacode.time.api.AlkaTimeAPI");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) {
                return null;
            }
            Method getOnlineSeconds = apiClass.getMethod("getOnlineSeconds", UUID.class);

            logger.info("AlkaTime detectado - requisitos de tempo online nos ranks vao funcionar.");
            return new TimeHook(registration.getProvider(), getOnlineSeconds, logger);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "AlkaTime encontrado mas a API nao carregou via reflexao (versao incompativel?).", t);
            return null;
        }
    }

    /** Segundos online (lifetime) do jogador, ou -1 se a chamada falhar. Nunca lanca. */
    public long getOnlineSeconds(UUID uuid) {
        try {
            CompletableFuture<?> future = (CompletableFuture<?>) getOnlineSecondsMethod.invoke(api, uuid);
            Object result = future.get(200, TimeUnit.MILLISECONDS);
            return result instanceof Number number ? number.longValue() : -1;
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook do AlkaTime falhou em getOnlineSeconds.", t);
            return -1;
        }
    }
}

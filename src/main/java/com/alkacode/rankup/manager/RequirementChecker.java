package com.alkacode.rankup.manager;

import com.alkacode.rankup.economy.EconomyService;
import com.alkacode.rankup.hook.TimeHook;
import com.alkacode.rankup.util.TimeUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Motor central de requisitos multi-moeda + tempo online (ver Rank#requirements). Cada
 * chave do mapa e um id de moeda do AlkaEconomy, OU a chave especial {@value #ONLINE_TIME_KEY}
 * (segundos, via AlkaTime). Usado tanto pelo RankUpService (checar/cobrar de verdade)
 * quanto pelas GUIs de rankup (so exibir o que falta, sem cobrar nada).
 */
public final class RequirementChecker {

    public static final String ONLINE_TIME_KEY = "online_time";
    /** Prefixo de chave pra requisito de cabeca (ex: "head:zombie") - roteado pro HeadsManager em vez do EconomyService. */
    public static final String HEAD_PREFIX = "head:";

    /** Um requisito nao cumprido - "have"/"need" na mesma unidade (segundos pra online_time, valor cru de moeda/cabecas pros outros). */
    public record Unmet(String key, double have, double need, boolean unavailable) {
    }

    private final EconomyService economyService;
    private final Supplier<TimeHook> timeHookSupplier;
    private final HeadsManager headsManager;

    public RequirementChecker(EconomyService economyService, Supplier<TimeHook> timeHookSupplier, HeadsManager headsManager) {
        this.economyService = economyService;
        this.timeHookSupplier = timeHookSupplier;
        this.headsManager = headsManager;
    }

    /** Lista de requisitos NAO cumpridos - vazia significa "pode ranquear agora". */
    public List<Unmet> checkUnmet(Player player, Map<String, Double> requirements) {
        List<Unmet> unmet = new ArrayList<>();
        for (Map.Entry<String, Double> entry : requirements.entrySet()) {
            String key = entry.getKey();
            double need = entry.getValue();

            if (ONLINE_TIME_KEY.equalsIgnoreCase(key)) {
                TimeHook hook = timeHookSupplier.get();
                if (hook == null) {
                    unmet.add(new Unmet(key, 0, need, true));
                    continue;
                }
                long have = hook.getOnlineSeconds(player.getUniqueId());
                if (have < 0) {
                    unmet.add(new Unmet(key, 0, need, true));
                } else if (have < need) {
                    unmet.add(new Unmet(key, have, need, false));
                }
                continue;
            }

            if (key.startsWith(HEAD_PREFIX)) {
                double have = headsManager.get(player.getUniqueId(), key.substring(HEAD_PREFIX.length()));
                if (have < need) {
                    unmet.add(new Unmet(key, have, need, false));
                }
                continue;
            }

            if (!economyService.isAvailable()) {
                unmet.add(new Unmet(key, 0, need, true));
                continue;
            }
            double have = economyService.getBalance(player, key);
            if (have < need) {
                unmet.add(new Unmet(key, have, need, false));
            }
        }
        return unmet;
    }

    /** So true se TODAS as fontes usadas pelos requisitos (economia, AlkaTime) estiverem prontas - independente de o jogador ter o suficiente ou nao. Heads sao locais, sempre disponiveis. */
    public boolean allSourcesAvailable(Map<String, Double> requirements) {
        for (String key : requirements.keySet()) {
            if (ONLINE_TIME_KEY.equalsIgnoreCase(key)) {
                if (timeHookSupplier.get() == null) {
                    return false;
                }
            } else if (!key.startsWith(HEAD_PREFIX) && !economyService.isAvailable()) {
                return false;
            }
        }
        return true;
    }

    /** Cobra todos os requisitos de moeda/cabeca (nunca "cobra" online_time, so e checado) - so chamar depois de checkUnmet() vir vazio. */
    public void charge(Player player, Map<String, Double> requirements) {
        for (Map.Entry<String, Double> entry : requirements.entrySet()) {
            String key = entry.getKey();
            if (ONLINE_TIME_KEY.equalsIgnoreCase(key)) {
                continue;
            }
            if (key.startsWith(HEAD_PREFIX)) {
                headsManager.consume(player.getUniqueId(), key.substring(HEAD_PREFIX.length()), (int) Math.round(entry.getValue()));
                continue;
            }
            economyService.withdraw(player, key, entry.getValue());
        }
    }

    /** % geral de progresso (0-100), somando min(have,need)/need de cada requisito - usado pra
     * barra de progresso na GUI. Requisito com a fonte indisponivel conta como 0% daquele item
     * (nao trava em 100% so porque a fonte caiu). */
    public int progressPercent(Player player, Map<String, Double> requirements) {
        if (requirements.isEmpty()) {
            return 100;
        }
        double haveSum = 0;
        double needSum = 0;
        for (Map.Entry<String, Double> entry : requirements.entrySet()) {
            String key = entry.getKey();
            double need = entry.getValue();
            double have;
            if (ONLINE_TIME_KEY.equalsIgnoreCase(key)) {
                TimeHook hook = timeHookSupplier.get();
                have = hook != null ? Math.max(0, hook.getOnlineSeconds(player.getUniqueId())) : 0;
            } else if (key.startsWith(HEAD_PREFIX)) {
                have = headsManager.get(player.getUniqueId(), key.substring(HEAD_PREFIX.length()));
            } else {
                have = economyService.isAvailable() ? economyService.getBalance(player, key) : 0;
            }
            haveSum += Math.min(have, need);
            needSum += need;
        }
        return needSum <= 0 ? 100 : (int) Math.round((haveSum / needSum) * 100);
    }

    /** "30.000.000 gold, 20.000 escarion, 80h online, 50 head:zombie" - pra lore da GUI e mensagens. */
    public String formatSummary(Map<String, Double> requirements) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Double> entry : requirements.entrySet()) {
            if (ONLINE_TIME_KEY.equalsIgnoreCase(entry.getKey())) {
                parts.add(TimeUtil.formatDuration((long) entry.getValue().doubleValue()) + " online");
            } else {
                parts.add(formatAmount(entry.getValue()) + " " + entry.getKey());
            }
        }
        return String.join(", ", parts);
    }

    /** Mesmo formato do formatSummary, mas so com os itens que faltam (pra mensagem de erro). */
    public String formatUnmet(List<Unmet> unmet) {
        Map<String, Double> asRequirements = new LinkedHashMap<>();
        for (Unmet entry : unmet) {
            asRequirements.put(entry.key(), entry.need());
        }
        return formatSummary(asRequirements);
    }

    private String formatAmount(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.of("pt", "BR"), "%,d", (long) value);
        }
        return String.format(Locale.of("pt", "BR"), "%,.2f", value);
    }
}

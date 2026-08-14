package com.alkacode.rankup.hook;

import com.alkacode.rankup.config.ConfigManager;
import com.alkacode.rankup.manager.KitCooldownManager;
import com.alkacode.rankup.manager.PlayerDataManager;
import com.alkacode.rankup.manager.RankManager;
import com.alkacode.rankup.manager.RequirementChecker;
import com.alkacode.rankup.model.KitType;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.TextUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Expoe o estado de rank/prestigio/progresso do jogador via PlaceholderAPI, para
 * consumo por plugins externos (ex: LeafScore no scoreboard). So cobre jogadores
 * online: {@code onPlaceholderRequest(Player, ...)} e o unico metodo sobrescrito,
 * entao consultas para OfflinePlayer (fora de {@link Player}) retornam null.
 */
public final class RankUpExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final PlayerDataManager playerDataManager;
    private final ConfigManager configManager;
    private final KitCooldownManager kitCooldownManager;
    private final RequirementChecker requirementChecker;

    public RankUpExpansion(JavaPlugin plugin, RankManager rankManager, PlayerDataManager playerDataManager,
                            ConfigManager configManager, KitCooldownManager kitCooldownManager,
                            RequirementChecker requirementChecker) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.playerDataManager = playerDataManager;
        this.configManager = configManager;
        this.kitCooldownManager = kitCooldownManager;
        this.requirementChecker = requirementChecker;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "alkarankup";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MestreDEV";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        PlayerRankData data = playerDataManager.get(player.getUniqueId());
        Rank current = rankManager.rankAt(data.rankIndex());
        String key = params.toLowerCase();

        if (key.startsWith("kit_")) {
            return kitCooldown(player, key);
        }

        return switch (key) {
            case "rank" -> current.displayName();
            case "rank_id" -> current.id();
            case "rank_index" -> String.valueOf(data.rankIndex());
            case "max_rank" -> String.valueOf(rankManager.maxIndex());
            case "prestige" -> String.valueOf(data.prestigeLevel());
            case "prestige_cost" -> String.valueOf(Math.round(
                    configManager.prestigeBaseCost() * Math.pow(configManager.prestigeCostGrowth(), data.prestigeLevel())));
            // next_cost/next_cost_formatted viraram a lista inteira de requisitos - um
            // rank multi-requisito nao tem mais "um" custo so pra mostrar como numero cru.
            case "next_cost", "next_cost_formatted", "next_requirements" -> rankManager.nextRank(data.rankIndex())
                    .map(next -> requirementChecker.formatSummary(next.requirements()))
                    .orElse("-");
            case "rank_name" -> TextUtil.plainText(current.displayName());
            case "rank_tag" -> TextUtil.legacy(current.displayName());
            case "next_rank" -> rankManager.nextRank(data.rankIndex())
                    .map(next -> TextUtil.plainText(next.displayName()))
                    .orElse(TextUtil.plainText(current.displayName()));
            case "progress" -> progress(player, data);
            default -> null;
        };
    }

    /** %alkarankup_kit_<rank_id>_<daily|weekly|monthly>_cooldown% - segundos restantes (0 = disponivel). */
    private String kitCooldown(Player player, String key) {
        String[] parts = key.split("_");
        if (parts.length < 3 || !parts[parts.length - 1].equals("cooldown")) {
            return null;
        }
        KitType type;
        try {
            type = KitType.valueOf(parts[parts.length - 2].toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        String rankId = String.join("_", java.util.Arrays.asList(parts).subList(1, parts.length - 2));

        Rank rank = rankManager.byId(rankId).orElse(null);
        if (rank == null) {
            return "0";
        }
        return rank.kit(type).map(kit -> {
            long lastClaimed = kitCooldownManager.lastClaimed(player.getUniqueId(), rank.id(), type);
            if (lastClaimed == 0) {
                return "0";
            }
            long elapsed = (System.currentTimeMillis() / 1000) - lastClaimed;
            long remaining = kit.cooldownSeconds() - elapsed;
            return String.valueOf(Math.max(0, remaining));
        }).orElse("0");
    }

    /** % de progresso pro proximo rank - o requisito com MENOR progresso manda (o "elo mais fraco" entre todos que precisam ser cumpridos juntos). */
    private String progress(Player player, PlayerRankData data) {
        return rankManager.nextRank(data.rankIndex())
                .map(next -> {
                    Map<String, Double> requirements = next.requirements();
                    if (requirements.isEmpty()) {
                        return "100.00%";
                    }
                    double worst = 100.0;
                    for (RequirementChecker.Unmet unmet : requirementChecker.checkUnmet(player, requirements)) {
                        double percent = unmet.unavailable() || unmet.need() <= 0
                                ? 0.0
                                : Math.min(100.0, (unmet.have() / unmet.need()) * 100.0);
                        worst = Math.min(worst, percent);
                    }
                    return String.format("%.2f%%", worst);
                })
                .orElse("100.00%");
    }
}

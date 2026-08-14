package com.alkacode.rankup.manager;

import com.alkacode.rankup.config.ConfigManager;
import com.alkacode.rankup.economy.EconomyService;
import com.alkacode.rankup.event.RankUpEvent;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.LuckPermsSync;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public final class RankUpService {

    public enum Status {
        SUCCESS,
        MAX_RANK,
        REQUIREMENTS_NOT_MET,
        REQUIREMENTS_UNAVAILABLE,
        CANCELLED
    }

    public record Result(Status status, Rank rank, List<RequirementChecker.Unmet> unmet) {
    }

    private final RankManager rankManager;
    private final PlayerDataManager playerDataManager;
    private final ConfigManager configManager;
    private final RequirementChecker requirementChecker;

    public RankUpService(RankManager rankManager, PlayerDataManager playerDataManager, ConfigManager configManager,
                          RequirementChecker requirementChecker) {
        this.rankManager = rankManager;
        this.playerDataManager = playerDataManager;
        this.configManager = configManager;
        this.requirementChecker = requirementChecker;
    }

    /**
     * O rankup agora e apenas a transacao economica (multi-requisito) + avanco de tier
     * + sync do grupo do LuckPerms. As recompensas de kit sao resgatadas manualmente
     * pelo jogador no sub-menu de kits (ver {@link com.alkacode.rankup.gui.KitMenu}),
     * so do rank ATUAL (ranks anteriores nao ficam mais acessiveis).
     */
    public Result attempt(Player player) {
        PlayerRankData data = playerDataManager.get(player.getUniqueId());

        if (rankManager.isMaxRank(data.rankIndex())) {
            Rank current = rankManager.rankAt(data.rankIndex());
            return new Result(Status.MAX_RANK, current, List.of());
        }

        Rank current = rankManager.rankAt(data.rankIndex());
        Rank next = rankManager.nextRank(data.rankIndex()).orElseThrow();

        if (!requirementChecker.allSourcesAvailable(next.requirements())) {
            return new Result(Status.REQUIREMENTS_UNAVAILABLE, next, List.of());
        }

        List<RequirementChecker.Unmet> unmet = requirementChecker.checkUnmet(player, next.requirements());
        if (!unmet.isEmpty()) {
            return new Result(Status.REQUIREMENTS_NOT_MET, next, unmet);
        }

        RankUpEvent event = new RankUpEvent(player, current, next);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return new Result(Status.CANCELLED, next, List.of());
        }

        requirementChecker.charge(player, next.requirements());
        data.setRankIndex(next.index());
        playerDataManager.persist(data);

        if (configManager.luckPermsSyncEnabled()) {
            LuckPermsSync.swapGroup(player.getName(), current.lpGroup(), next.lpGroup());
        }

        dispatchCommands(player, next);
        return new Result(Status.SUCCESS, next, List.of());
    }

    private void dispatchCommands(Player player, Rank rank) {
        for (String command : configManager.rankupCommands()) {
            String parsed = command
                    .replace("%player%", player.getName())
                    .replace("%rank%", rank.id());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }
}

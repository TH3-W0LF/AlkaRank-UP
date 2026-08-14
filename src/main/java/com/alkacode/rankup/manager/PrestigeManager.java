package com.alkacode.rankup.manager;

import com.alkacode.rankup.config.ConfigManager;
import com.alkacode.rankup.economy.EconomyService;
import com.alkacode.rankup.event.PrestigeEvent;
import com.alkacode.rankup.model.PlayerRankData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PrestigeManager {

    public enum Status {
        SUCCESS,
        NOT_MAX_RANK,
        INSUFFICIENT_FUNDS,
        ECONOMY_UNAVAILABLE,
        CANCELLED
    }

    public record Result(Status status, int prestigeLevel, double cost) {
    }

    private final RankManager rankManager;
    private final PlayerDataManager playerDataManager;
    private final EconomyService economyService;
    private final ConfigManager configManager;

    public PrestigeManager(RankManager rankManager, PlayerDataManager playerDataManager,
                            EconomyService economyService, ConfigManager configManager) {
        this.rankManager = rankManager;
        this.playerDataManager = playerDataManager;
        this.economyService = economyService;
        this.configManager = configManager;
    }

    /** So checa se o jogador PODE tentar prestigiar agora, sem cobrar nada - usado antes de abrir a GUI de confirmacao. */
    public Status checkEligibility(Player player) {
        PlayerRankData data = playerDataManager.get(player.getUniqueId());
        if (!economyService.isAvailable()) {
            return Status.ECONOMY_UNAVAILABLE;
        }
        if (!rankManager.isMaxRank(data.rankIndex())) {
            return Status.NOT_MAX_RANK;
        }
        return Status.SUCCESS;
    }

    /** Custo pra prestigiar no nivel atual do jogador - usado pela GUI de confirmacao antes de tentar de verdade. */
    public double previewCost(Player player) {
        PlayerRankData data = playerDataManager.get(player.getUniqueId());
        return configManager.prestigeBaseCost() * Math.pow(configManager.prestigeCostGrowth(), data.prestigeLevel());
    }

    public Result attempt(Player player) {
        PlayerRankData data = playerDataManager.get(player.getUniqueId());

        if (!economyService.isAvailable()) {
            return new Result(Status.ECONOMY_UNAVAILABLE, data.prestigeLevel(), 0.0);
        }

        if (!rankManager.isMaxRank(data.rankIndex())) {
            return new Result(Status.NOT_MAX_RANK, data.prestigeLevel(), 0.0);
        }

        double cost = previewCost(player);
        String currency = configManager.defaultCurrency();
        if (!economyService.has(player, currency, cost)) {
            return new Result(Status.INSUFFICIENT_FUNDS, data.prestigeLevel(), cost);
        }

        int newPrestigeLevel = data.prestigeLevel() + 1;
        PrestigeEvent event = new PrestigeEvent(player, newPrestigeLevel, cost);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return new Result(Status.CANCELLED, data.prestigeLevel(), cost);
        }

        if (cost > 0) {
            economyService.withdraw(player, currency, cost);
        }

        data.setRankIndex(0);
        data.setPrestigeLevel(newPrestigeLevel);
        playerDataManager.persist(data);

        dispatchCommands(player, newPrestigeLevel);
        return new Result(Status.SUCCESS, newPrestigeLevel, cost);
    }

    private void dispatchCommands(Player player, int prestigeLevel) {
        for (String command : configManager.prestigeCommands()) {
            String parsed = command
                    .replace("%player%", player.getName())
                    .replace("%prestige_level%", String.valueOf(prestigeLevel));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }
}

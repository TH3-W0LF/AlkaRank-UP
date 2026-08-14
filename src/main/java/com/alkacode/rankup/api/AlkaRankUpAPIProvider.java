package com.alkacode.rankup.api;

import com.alkacode.rankup.config.ConfigManager;
import com.alkacode.rankup.manager.PlayerDataManager;
import com.alkacode.rankup.model.PlayerRankData;

import java.util.UUID;

public final class AlkaRankUpAPIProvider implements AlkaRankUpAPI {

    private final PlayerDataManager playerDataManager;
    private final ConfigManager configManager;

    public AlkaRankUpAPIProvider(PlayerDataManager playerDataManager, ConfigManager configManager) {
        this.playerDataManager = playerDataManager;
        this.configManager = configManager;
    }

    @Override
    public double getSellMultiplier(UUID uuid) {
        PlayerRankData data = playerDataManager.get(uuid);
        return 1.0 + (configManager.sellBonusPerPrestige() * data.prestigeLevel());
    }
}

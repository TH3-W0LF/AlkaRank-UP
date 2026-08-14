package com.alkacode.rankup.manager;

import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.rankup.database.RankDataRepository;
import com.alkacode.rankup.model.PlayerRankData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {

    private final RankDataRepository repository;
    private final AlkaScheduler scheduler;
    private final Map<UUID, PlayerRankData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(RankDataRepository repository, AlkaScheduler scheduler) {
        this.repository = repository;
        this.scheduler = scheduler;
    }

    public void loadForJoin(Player player) {
        UUID uuid = player.getUniqueId();
        scheduler.runAsync(() -> cache.put(uuid, repository.loadOrInit(uuid)));
    }

    public PlayerRankData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, repository::loadOrInit);
    }

    public void saveAndForget(UUID uuid) {
        PlayerRankData data = cache.remove(uuid);
        if (data != null && data.isDirty()) {
            scheduler.runAsync(() -> repository.save(data));
        }
    }

    public void persist(PlayerRankData data) {
        scheduler.runAsync(() -> repository.save(data));
    }

    public void saveAllSync() {
        for (PlayerRankData data : cache.values()) {
            if (data.isDirty()) {
                repository.save(data);
            }
        }
    }

    public List<PlayerRankData> topPlayersSync(int limit) {
        return repository.topPlayers(limit);
    }
}

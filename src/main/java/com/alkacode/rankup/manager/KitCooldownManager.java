package com.alkacode.rankup.manager;

import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.rankup.database.KitCooldownRepository;
import com.alkacode.rankup.model.KitType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memoria dos cooldowns de kit por jogador, pre-carregado inteiro no join
 * (uma unica query) e liberado no quit. Cada resgate grava de imediato no banco -
 * nao ha necessidade de dirty-tracking como em {@link PlayerDataManager}, pois um
 * cooldown so muda no exato momento em que o kit e resgatado.
 */
public final class KitCooldownManager {

    private final KitCooldownRepository repository;
    private final AlkaScheduler scheduler;
    private final Map<UUID, Map<String, Long>> cache = new ConcurrentHashMap<>();

    public KitCooldownManager(KitCooldownRepository repository, AlkaScheduler scheduler) {
        this.repository = repository;
        this.scheduler = scheduler;
    }

    private static String key(String rankId, KitType kitType) {
        return rankId + ":" + kitType.name();
    }

    public void loadForJoin(Player player) {
        UUID uuid = player.getUniqueId();
        scheduler.runAsync(() -> cache.put(uuid, new ConcurrentHashMap<>(repository.loadCooldowns(uuid))));
    }

    public void evict(UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * @return epoch millis do ultimo resgate, ou 0 se o jogador nunca resgatou este kit.
     */
    public long lastClaimed(UUID uuid, String rankId, KitType kitType) {
        Map<String, Long> playerCooldowns = cache.get(uuid);
        if (playerCooldowns == null) {
            return repository.loadCooldowns(uuid).getOrDefault(key(rankId, kitType), 0L);
        }
        return playerCooldowns.getOrDefault(key(rankId, kitType), 0L);
    }

    public void markClaimed(UUID uuid, String rankId, KitType kitType, long timestamp) {
        cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>()).put(key(rankId, kitType), timestamp);
        scheduler.runAsync(() -> repository.saveCooldown(uuid, rankId, kitType, timestamp));
    }
}

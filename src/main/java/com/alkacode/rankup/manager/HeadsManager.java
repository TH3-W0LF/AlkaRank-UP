package com.alkacode.rankup.manager;

import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.rankup.database.HeadsRepository;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armazenamento de cabecas depositadas por jogador - cache em memoria + persistencia
 * via {@link HeadsRepository}. Mesma forma de PlayerDataManager/KitCooldownManager:
 * carrega tudo no join, escreve na hora a cada deposito/consumo (sem dirty-tracking,
 * cada mutacao ja e o proprio evento que precisa ser persistido).
 */
public final class HeadsManager {

    private final HeadsRepository repository;
    private final AlkaScheduler scheduler;
    private final Map<UUID, Map<String, Integer>> cache = new ConcurrentHashMap<>();

    public HeadsManager(HeadsRepository repository, AlkaScheduler scheduler) {
        this.repository = repository;
        this.scheduler = scheduler;
    }

    public void loadForJoin(Player player) {
        UUID uuid = player.getUniqueId();
        scheduler.runAsync(() -> cache.put(uuid, new ConcurrentHashMap<>(repository.loadBanked(uuid))));
    }

    public void evict(UUID uuid) {
        cache.remove(uuid);
    }

    /** Quantidade bancada atual de um tipo de cabeca (0 se nunca depositou nenhuma). */
    public int get(UUID uuid, String headId) {
        Map<String, Integer> playerHeads = cache.get(uuid);
        if (playerHeads == null) {
            return repository.loadBanked(uuid).getOrDefault(headId, 0);
        }
        return playerHeads.getOrDefault(headId, 0);
    }

    public Map<String, Integer> all(UUID uuid) {
        Map<String, Integer> playerHeads = cache.get(uuid);
        return playerHeads == null ? repository.loadBanked(uuid) : Map.copyOf(playerHeads);
    }

    public void deposit(UUID uuid, String headId, int amount) {
        if (amount <= 0) {
            return;
        }
        int newAmount = cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>())
                .merge(headId, amount, Integer::sum);
        scheduler.runAsync(() -> repository.saveAmount(uuid, headId, newAmount));
    }

    /** Consome do banco (usado ao cobrar um requisito de rankup) - so chamar depois de confirmar saldo suficiente. */
    public void consume(UUID uuid, String headId, int amount) {
        if (amount <= 0) {
            return;
        }
        int newAmount = Math.max(0, get(uuid, headId) - amount);
        cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>()).put(headId, newAmount);
        scheduler.runAsync(() -> repository.saveAmount(uuid, headId, newAmount));
    }
}

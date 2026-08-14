package com.alkacode.rankup.listener;

import com.alkacode.rankup.manager.HeadsManager;
import com.alkacode.rankup.manager.KitCooldownManager;
import com.alkacode.rankup.manager.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final KitCooldownManager kitCooldownManager;
    private final HeadsManager headsManager;

    public PlayerConnectionListener(PlayerDataManager playerDataManager, KitCooldownManager kitCooldownManager,
                                     HeadsManager headsManager) {
        this.playerDataManager = playerDataManager;
        this.kitCooldownManager = kitCooldownManager;
        this.headsManager = headsManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handle(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerDataManager.saveAndForget(event.getPlayer().getUniqueId());
        kitCooldownManager.evict(event.getPlayer().getUniqueId());
        headsManager.evict(event.getPlayer().getUniqueId());
    }

    /**
     * Publico para tambem cobrir jogadores ja online quando o plugin e habilitado
     * (ex: apos /reload), que nao disparam PlayerJoinEvent de novo.
     */
    public void handle(Player player) {
        playerDataManager.loadForJoin(player);
        kitCooldownManager.loadForJoin(player);
        headsManager.loadForJoin(player);
    }
}

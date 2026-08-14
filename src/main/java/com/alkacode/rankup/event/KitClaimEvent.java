package com.alkacode.rankup.event;

import com.alkacode.rankup.model.Kit;
import com.alkacode.rankup.model.Rank;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Disparado ANTES de marcar o cooldown e entregar os itens do kit - cancelar impede o resgate inteiro. */
public final class KitClaimEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Rank rank;
    private final Kit kit;
    private boolean cancelled;

    public KitClaimEvent(Player player, Rank rank, Kit kit) {
        this.player = player;
        this.rank = rank;
        this.kit = kit;
    }

    public Player player() {
        return player;
    }

    public Rank rank() {
        return rank;
    }

    public Kit kit() {
        return kit;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

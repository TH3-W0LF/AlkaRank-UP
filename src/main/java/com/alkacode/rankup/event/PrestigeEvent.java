package com.alkacode.rankup.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Disparado ANTES de cobrar o jogador e resetar o rank pro prestigio - cancelar impede a transacao inteira. */
public final class PrestigeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int newPrestigeLevel;
    private final double cost;
    private boolean cancelled;

    public PrestigeEvent(Player player, int newPrestigeLevel, double cost) {
        this.player = player;
        this.newPrestigeLevel = newPrestigeLevel;
        this.cost = cost;
    }

    public Player player() {
        return player;
    }

    public int newPrestigeLevel() {
        return newPrestigeLevel;
    }

    public double cost() {
        return cost;
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

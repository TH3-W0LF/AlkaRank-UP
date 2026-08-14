package com.alkacode.rankup.event;

import com.alkacode.rankup.model.Rank;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Disparado ANTES de cobrar o jogador e avancar o rank - cancelar impede a transacao
 * inteira (nenhum requisito e cobrado, rank nao muda). Pensado pra integracoes de
 * terceiros (ex: AlkaMines checando um requisito extra proprio). Os requisitos ja
 * cobrados/checados estao em {@code toRank().requirements()}.
 */
public final class RankUpEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Rank fromRank;
    private final Rank toRank;
    private boolean cancelled;

    public RankUpEvent(Player player, Rank fromRank, Rank toRank) {
        this.player = player;
        this.fromRank = fromRank;
        this.toRank = toRank;
    }

    public Player player() {
        return player;
    }

    public Rank fromRank() {
        return fromRank;
    }

    public Rank toRank() {
        return toRank;
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

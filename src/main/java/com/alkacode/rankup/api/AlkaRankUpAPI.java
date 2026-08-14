package com.alkacode.rankup.api;

import java.util.UUID;

/** API publica minima do AlkaRankUp pra outros plugins Alka* consultarem beneficios de prestigio. */
public interface AlkaRankUpAPI {

    /** Multiplicador de venda do jogador baseado no prestigio (ex: 1.10 = +10%). 1.0 = sem bonus/sem prestigio. */
    double getSellMultiplier(UUID uuid);
}

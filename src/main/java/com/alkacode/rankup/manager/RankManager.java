package com.alkacode.rankup.manager;

import com.alkacode.rankup.model.Rank;

import java.util.List;
import java.util.Optional;

public final class RankManager {

    private List<Rank> ranks;

    public RankManager(List<Rank> ranks) {
        this.ranks = ranks;
    }

    /**
     * Troca a lista de ranks in-place - as GUIs/services (RankListMenu, KitMenu, etc) guardam
     * esta MESMA instancia de RankManager (nao uma copia), entao trocar o campo aqui
     * ja propaga o reload pra todo mundo sem precisar re-injetar nada.
     */
    public void reload(List<Rank> newRanks) {
        this.ranks = newRanks;
    }

    public List<Rank> ranks() {
        return ranks;
    }

    public Rank rankAt(int index) {
        return ranks.get(index);
    }

    public int maxIndex() {
        return ranks.size() - 1;
    }

    public boolean isMaxRank(int rankIndex) {
        return rankIndex >= maxIndex();
    }

    public Optional<Rank> nextRank(int rankIndex) {
        if (isMaxRank(rankIndex)) {
            return Optional.empty();
        }
        return Optional.of(ranks.get(rankIndex + 1));
    }

    public Optional<Rank> byId(String id) {
        return ranks.stream().filter(rank -> rank.id().equalsIgnoreCase(id)).findFirst();
    }
}

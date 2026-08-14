package com.alkacode.rankup.model;

import java.util.UUID;

public final class PlayerRankData {

    private final UUID uuid;
    private int rankIndex;
    private int prestigeLevel;
    private volatile boolean dirty;

    public PlayerRankData(UUID uuid, int rankIndex, int prestigeLevel) {
        this.uuid = uuid;
        this.rankIndex = rankIndex;
        this.prestigeLevel = prestigeLevel;
    }

    public UUID uuid() {
        return uuid;
    }

    public int rankIndex() {
        return rankIndex;
    }

    public int prestigeLevel() {
        return prestigeLevel;
    }

    public void setRankIndex(int rankIndex) {
        this.rankIndex = rankIndex;
        this.dirty = true;
    }

    public void setPrestigeLevel(int prestigeLevel) {
        this.prestigeLevel = prestigeLevel;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }
}

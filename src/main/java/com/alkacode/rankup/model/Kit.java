package com.alkacode.rankup.model;

import java.util.List;

public record Kit(KitType type, String name, long cooldownSeconds, IconConfig iconAvailable, IconConfig iconCooldown,
                   List<KitItem> items) {
}

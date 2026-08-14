package com.alkacode.rankup.model;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

public record KitItem(Material material, int amount, String displayName, List<String> lore, int customModelData,
                       Map<Enchantment, Integer> enchantments) {
}

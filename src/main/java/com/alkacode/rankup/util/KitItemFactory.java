package com.alkacode.rankup.util;

import com.alkacode.rankup.model.KitItem;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class KitItemFactory {

    private KitItemFactory() {
    }

    /**
     * Monta o ItemStack nativamente via API do Bukkit. Usa
     * {@link ItemStack#addUnsafeEnchantment} para permitir niveis de encantamento
     * acima do limite vanilla (ex: Protection 15), ja que o servidor roda plugins que
     * quebram esse teto.
     */
    public static ItemStack build(KitItem kitItem) {
        ItemStack item = new ItemStack(kitItem.material(), kitItem.amount());
        ItemMeta meta = item.getItemMeta();

        if (kitItem.displayName() != null) {
            meta.displayName(TextUtil.parse(kitItem.displayName()));
        }
        if (!kitItem.lore().isEmpty()) {
            meta.lore(TextUtil.parseList(kitItem.lore(), Map.of()));
        }
        IconFactory.applyCustomModelData(meta, kitItem.customModelData());

        item.setItemMeta(meta);

        for (Map.Entry<Enchantment, Integer> entry : kitItem.enchantments().entrySet()) {
            item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        return item;
    }
}

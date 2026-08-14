package com.alkacode.rankup.listener;

import com.alkacode.core.util.ItemBuilder;
import com.alkacode.rankup.config.ConfigManager;
import com.alkacode.rankup.model.HeadType;
import com.alkacode.rankup.util.TextUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Chance de dropar uma cabeca fisica (heads.types) ao matar um mob configurado. A
 * cabeca vai direto pro inventario do matador (ou cai no chao se o inventario estiver
 * cheio) - o deposito no banco (HeadsManager) e feito manualmente pelo jogador no
 * HeadsMenu, nunca creditado automaticamente aqui.
 */
public final class HeadDropListener implements Listener {

    public static final String HEAD_ID_KEY = "rankup_head_id";

    private final ConfigManager configManager;
    private final NamespacedKey headIdKey;

    public HeadDropListener(JavaPlugin plugin, ConfigManager configManager) {
        this.configManager = configManager;
        this.headIdKey = new NamespacedKey(plugin, HEAD_ID_KEY);
    }

    public NamespacedKey headIdKey() {
        return headIdKey;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!configManager.headsEnabled()) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        HeadType type = configManager.headTypeForEntity(event.getEntityType());
        if (type == null || type.chance() <= 0) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > type.chance()) {
            return;
        }

        ItemStack head = buildHead(type);
        if (configManager.headsGiveDirectlyToInventory()) {
            for (ItemStack leftover : killer.getInventory().addItem(head).values()) {
                killer.getWorld().dropItemNaturally(killer.getLocation(), leftover);
            }
        } else {
            killer.getWorld().dropItemNaturally(killer.getLocation(), head);
        }
    }

    private ItemStack buildHead(HeadType type) {
        ItemStack item = type.hasTexture() ? ItemBuilder.skullFromTexture(type.texture()) : new ItemStack(type.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse(type.displayName()));
        meta.getPersistentDataContainer().set(headIdKey, PersistentDataType.STRING, type.id());
        item.setItemMeta(meta);
        return item;
    }
}

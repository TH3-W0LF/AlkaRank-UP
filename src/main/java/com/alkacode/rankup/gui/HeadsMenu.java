package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.model.HeadType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/** Armazenamento de cabecas - mostra o banco por tipo e deposita tudo que o jogador carrega no inventario. */
public final class HeadsMenu extends BaseGui {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
    };

    private final RankUpServices services;

    public HeadsMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.rawMessage("gui.heads-title"), 6, "rankup_heads");
        this.services = services;
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        int i = 0;
        for (HeadType type : services.configManager.headTypes()) {
            if (i >= CONTENT_SLOTS.length) {
                break;
            }
            int banked = services.headsManager.get(player.getUniqueId(), type.id());
            String[] lore = services.configManager.rawMessageList("gui.heads-banked-lore").stream()
                    .map(line -> line.replace("<amount>", String.valueOf(banked)))
                    .toArray(String[]::new);
            ItemStack icon = type.hasTexture()
                    ? headTexture(type.texture(), type.displayName(), lore)
                    : createItem(type.material(), type.displayName(), lore);
            setItem(CONTENT_SLOTS[i], icon, null);
            i++;
        }

        setItem(49, createItem(Material.HOPPER, services.configManager.rawMessage("gui.heads-deposit-name"),
                services.configManager.rawMessageList("gui.heads-deposit-lore").toArray(new String[0])),
                e -> depositAll());

        int backSlot = services.configManager.backButtonSlot();
        setItem(backSlot >= 0 ? backSlot : 45, createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name")),
                e -> new RankMainMenu(player, services).open());
    }

    /** Varre o inventario procurando itens com a PDC tag do HeadDropListener e deposita tudo no banco. */
    private void depositAll() {
        PlayerInventory inventory = player.getInventory();
        Map<String, Integer> collected = new HashMap<>();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || !item.hasItemMeta()) {
                continue;
            }
            String headId = item.getItemMeta().getPersistentDataContainer().get(services.headIdKey, PersistentDataType.STRING);
            if (headId == null) {
                continue;
            }
            collected.merge(headId, item.getAmount(), Integer::sum);
            inventory.setItem(slot, null);
        }

        int total = collected.values().stream().mapToInt(Integer::intValue).sum();
        for (Map.Entry<String, Integer> entry : collected.entrySet()) {
            services.headsManager.deposit(player.getUniqueId(), entry.getKey(), entry.getValue());
        }
        services.messages.send(player, total > 0 ? "heads.deposited" : "heads.nothing-to-deposit",
                Map.of("amount", String.valueOf(total)));
        refresh();
    }
}

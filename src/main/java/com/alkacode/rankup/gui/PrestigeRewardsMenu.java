package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.model.KitItem;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.util.KitItemFactory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Grade de recompensas UNICAS por nivel de prestigio - cada nivel so pode ser resgatado uma vez (ver PrestigeRewardsRepository). */
public final class PrestigeRewardsMenu extends BaseGui {

    private static final int FIRST_SLOT = 10;

    private final RankUpServices services;

    public PrestigeRewardsMenu(Player player, RankUpServices services) {
        super(services.plugin, player, "<light_purple><b>Recompensas de Prestigio</b></light_purple>", 3, "rankup_prestige_rewards");
        this.services = services;
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        List<Integer> levels = services.configManager.prestigeRewardLevels();
        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        Set<Integer> claimed = services.prestigeRewardsRepository.loadClaimed(player.getUniqueId());

        int slot = FIRST_SLOT;
        for (int level : levels) {
            setItem(slot, buildIcon(level, data.prestigeLevel(), claimed.contains(level)), e -> handleClick(level));
            slot++;
        }

        // Slot fixo (nao usa o back_button.slot global) - esse menu tem 27 slots (3 linhas),
        // o slot global (27) e calibrado pros menus de kit de 36 slots e ficaria fora do range aqui.
        setItem(22, createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name")),
                e -> new RankMainMenu(player, services).open());
    }

    private void handleClick(int level) {
        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        if (data.prestigeLevel() < level) {
            return;
        }
        if (services.prestigeRewardsRepository.loadClaimed(player.getUniqueId()).contains(level)) {
            return;
        }

        for (KitItem kitItem : services.configManager.prestigeRewardItems(level)) {
            ItemStack stack = KitItemFactory.build(kitItem);
            for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        services.prestigeRewardsRepository.markClaimed(player.getUniqueId(), level);
        services.messages.send(player, "prestige.reward-claimed", Map.of("level", String.valueOf(level)));

        refresh();
    }

    private ItemStack buildIcon(int level, int playerPrestige, boolean claimed) {
        List<KitItem> items = services.configManager.prestigeRewardItems(level);
        Material previewMaterial = items.isEmpty() ? Material.CHEST : items.get(0).material();

        if (claimed) {
            return createItem(Material.LIME_STAINED_GLASS_PANE, "<light_purple><b>Prestigio " + level + "</b></light_purple>",
                    "<green>Ja resgatado.");
        }
        if (playerPrestige >= level) {
            return createItem(previewMaterial, "<light_purple><b>Prestigio " + level + "</b></light_purple>",
                    "<yellow>Clique para resgatar!");
        }
        return createItem(Material.BARRIER, "<light_purple><b>Prestigio " + level + "</b></light_purple>",
                "<red>Alcance o Prestigio " + level + " primeiro.");
    }
}

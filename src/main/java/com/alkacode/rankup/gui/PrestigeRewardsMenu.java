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

    private final RankUpServices services;

    public PrestigeRewardsMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.rawMessage("gui.prestige-rewards-title"), 3, "rankup_prestige_rewards");
        this.services = services;
    }

    @Override
    public void render() {
        var layout = services.guiLayoutLoader.getLayout("rankup_prestige_rewards");
        fillBorder(createItem(services.configManager.material("rankup_prestige_rewards.filler", Material.GRAY_STAINED_GLASS_PANE), " "));

        List<Integer> levels = services.configManager.prestigeRewardLevels();
        List<Integer> contentSlots = layout.findSlots('0');
        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        Set<Integer> claimed = services.prestigeRewardsRepository.loadClaimed(player.getUniqueId());

        for (int i = 0; i < levels.size() && i < contentSlots.size(); i++) {
            int level = levels.get(i);
            setItem(contentSlots.get(i), buildIcon(level, data.prestigeLevel(), claimed.contains(level)), e -> handleClick(level));
        }

        setItem(layout.firstSlot('V'), createItem(services.configManager.material("rankup_prestige_rewards.back", Material.ARROW),
                services.configManager.rawMessage("gui.back-name")),
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
        String name = services.configManager.rawMessage("gui.prestige-rewards-name").replace("<level>", String.valueOf(level));

        if (claimed) {
            return createItem(services.configManager.material("rankup_prestige_rewards.claimed", Material.LIME_STAINED_GLASS_PANE),
                    name, services.configManager.rawMessage("gui.prestige-rewards-claimed-lore"));
        }
        if (playerPrestige >= level) {
            return createItem(previewMaterial, name, services.configManager.rawMessage("gui.prestige-rewards-available-lore"));
        }
        return createItem(services.configManager.material("rankup_prestige_rewards.locked", Material.BARRIER), name,
                services.configManager.rawMessage("gui.prestige-rewards-locked-lore").replace("<level>", String.valueOf(level)));
    }
}

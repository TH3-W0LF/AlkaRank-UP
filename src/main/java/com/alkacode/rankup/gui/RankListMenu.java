package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

/** Grade de todos os ranks (locked/atual/completo/proximo-com-custo) - clicar no rank ATUAL abre os kits dele. */
public final class RankListMenu extends BaseGui {

    private final RankUpServices services;

    public RankListMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.guiSection().getString("title", "<dark_gray>Ranks</dark_gray>"),
                Math.max(1, services.configManager.guiSection().getInt("size", 54) / 9), "rankup_list");
        this.services = services;
    }

    @Override
    public void render() {
        ConfigurationSection gui = services.configManager.guiSection();
        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        List<Integer> rankSlots = gui.getIntegerList("rank-slots");

        for (Rank rank : services.rankManager.ranks()) {
            if (rank.index() >= rankSlots.size()) {
                continue;
            }
            int slot = rankSlots.get(rank.index());
            setItem(slot, buildRankItem(rank, data), e -> handleClick(rank, data));
        }

        int backSlot = services.configManager.backButtonSlot();
        if (backSlot >= 0) {
            setItem(backSlot, createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name")),
                    e -> new RankMainMenu(player, services).open());
        }

        if (services.configManager.fillEmptySlotsEnabled()) {
            var icon = services.configManager.fillEmptySlotsIcon();
            fill(createItem(icon.material(), icon.displayName() != null ? icon.displayName() : " "));
        }
    }

    private void handleClick(Rank rank, PlayerRankData data) {
        if (rank.index() == data.rankIndex() + 1 && !services.rankManager.isMaxRank(data.rankIndex())) {
            new RankUpConfirmMenu(player, services).open();
        } else if (rank.index() == data.rankIndex()) {
            // So o rank ATUAL tem kit acessivel - ranquear de novo troca o kit
            // disponivel, os de ranks anteriores nao ficam mais alcancaveis.
            new KitMenu(player, services, rank.id()).open();
        }
    }

    private ItemStack buildRankItem(Rank rank, PlayerRankData data) {
        List<String> loreLines;
        Map<String, String> placeholders = Map.of();
        if (rank.index() < data.rankIndex()) {
            loreLines = services.configManager.rawMessageList("gui.completed-lore");
        } else if (rank.index() == data.rankIndex()) {
            loreLines = services.configManager.rawMessageList("gui.current-lore");
        } else if (rank.index() == data.rankIndex() + 1) {
            loreLines = services.configManager.rawMessageList("gui.next-lore");
            placeholders = Map.of("cost", services.requirementChecker.formatSummary(rank.requirements()));
        } else {
            loreLines = services.configManager.rawMessageList("gui.locked-lore");
        }

        ItemStack item = new ItemStack(rank.icon().material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse(rank.displayName()));
        meta.lore(TextUtil.parseList(loreLines, placeholders));
        item.setItemMeta(meta);
        return item;
    }
}

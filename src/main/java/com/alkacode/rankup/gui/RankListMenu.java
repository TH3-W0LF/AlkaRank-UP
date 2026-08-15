package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.model.Kit;
import com.alkacode.rankup.model.KitType;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Grade de todos os ranks (locked/atual/completo/proximo-com-progresso) - clicar no rank ATUAL abre os kits dele. */
public final class RankListMenu extends BaseGui {

    /** Capacidade fixa do layout (2 linhas de 7) - ver AlkaRankUpPlugin#onEnable e
     * RankUpCommand#handleAdmin, que avisam no log/console se houver mais ranks configurados
     * do que isso (os excedentes simplesmente nao aparecem na grade). */
    public static final int MAX_RANKS = 14;

    private static final String[] LAYOUT = {
            "GGGGGGGGG",
            "G1234567G",
            "G89abcdeG",
            "G_______G",
            "GGGGVGGGG",
            "GGGGGGGGG",
    };

    private static final char[] RANK_SLOTS = {'1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e'};

    private final RankUpServices services;

    public RankListMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.rawMessage("gui.rank-list-title"), 6, "rankup_list");
        this.services = services;
    }

    @Override
    public void render() {
        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        List<Rank> ranks = services.rankManager.ranks();

        Map<Character, ItemStack> icons = new HashMap<>();
        Map<Character, Consumer<InventoryClickEvent>> clicks = new HashMap<>();

        icons.put('G', createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        icons.put('V', createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name"),
                lore("gui.back-lore")));
        clicks.put('V', e -> new RankMainMenu(player, services).open());

        for (int i = 0; i < RANK_SLOTS.length && i < ranks.size(); i++) {
            char c = RANK_SLOTS[i];
            Rank rank = ranks.get(i);
            icons.put(c, buildRankItem(rank, data));
            clicks.put(c, e -> handleClick(rank, data));
        }

        layout(LAYOUT, icons, clicks);
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
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<rank>", rank.displayName());
        List<String> loreLines;

        if (rank.index() < data.rankIndex()) {
            loreLines = services.configManager.rawMessageList("gui.rank-list.completed-lore");
        } else if (rank.index() == data.rankIndex()) {
            loreLines = services.configManager.rawMessageList("gui.rank-list.current-lore");
            placeholders.put("<kits_available>", String.valueOf(countAvailableKits(rank)));
        } else if (rank.index() == data.rankIndex() + 1) {
            loreLines = services.configManager.rawMessageList("gui.rank-list.next-lore");
            placeholders.put("<cost>", services.requirementChecker.formatSummary(rank.requirements()));
            placeholders.put("<progress>", progressBar(services.requirementChecker.progressPercent(player, rank.requirements())));
        } else {
            loreLines = services.configManager.rawMessageList("gui.rank-list.locked-lore");
            placeholders.put("<required_rank>", services.rankManager.rankAt(rank.index() - 1).displayName());
        }

        ItemStack item = com.alkacode.rankup.util.IconFactory.build(rank.icon(), null, List.of());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse(rank.displayName()));
        meta.lore(replacePlaceholders(loreLines, placeholders));
        item.setItemMeta(meta);
        return item;
    }

    /** Quantos kits deste rank o jogador pode resgatar AGORA (fora de cooldown). */
    private int countAvailableKits(Rank rank) {
        long now = System.currentTimeMillis() / 1000;
        int count = 0;
        for (KitType type : KitType.values()) {
            Kit kit = rank.kit(type).orElse(null);
            if (kit == null) {
                continue;
            }
            long lastClaimed = services.kitCooldownManager.lastClaimed(player.getUniqueId(), rank.id(), type);
            if (lastClaimed == 0 || now - lastClaimed >= kit.cooldownSeconds()) {
                count++;
            }
        }
        return count;
    }

    /** Barra de texto (dez blocos) + percentual, ex: "<green>██████<gray>████ <white>60%". */
    static String progressBar(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        int filled = Math.round(clamped / 10f);
        return "<green>" + "█".repeat(filled) + "<gray>" + "█".repeat(10 - filled) + " <white>" + clamped + "%";
    }

    /** Substitui <chave> pelo valor ANTES de mandar pro MiniMessage - RequirementChecker#formatSummary
     * ja pode devolver texto cru (nao MiniMessage), entao a ordem importa: placeholder primeiro. */
    static List<Component> replacePlaceholders(List<String> lines, Map<String, String> placeholders) {
        List<Component> result = new ArrayList<>();
        for (String line : lines) {
            String resolved = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                resolved = resolved.replace(entry.getKey(), entry.getValue());
            }
            result.add(TextUtil.parse(resolved));
        }
        return result;
    }

    private String[] lore(String path) {
        return services.configManager.rawMessageList(path).toArray(new String[0]);
    }
}

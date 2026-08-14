package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

/** Leaderboard paginado (maior prestigio primeiro, rank como desempate) - mesmo template de paginacao do AlkaMines.RankingGui. */
public final class TopMenu extends BaseGui {

    private static final int PER_PAGE = 7;
    private static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private final RankUpServices services;
    private int page;

    public TopMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.rawMessage("gui.top-title"), 6, "rankup_top");
        this.services = services;
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        List<PlayerRankData> top = services.playerDataManager.topPlayersSync((page + 1) * PER_PAGE);
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= top.size()) {
                break;
            }
            PlayerRankData data = top.get(index);
            OfflinePlayer target = Bukkit.getOfflinePlayer(data.uuid());
            Rank rank = services.rankManager.rankAt(data.rankIndex());
            String name = target.getName() != null ? target.getName() : "???";

            setItem(CONTENT_SLOTS[i], head(name, "<gold>#" + (index + 1) + " <white>" + name,
                    services.configManager.rawMessageList("gui.top-entry-lore").stream()
                            .map(line -> line.replace("<rank>", rank.displayName())
                                    .replace("<prestige>", String.valueOf(data.prestigeLevel())))
                            .toArray(String[]::new)));
        }

        boolean hasPrevious = page > 0;
        boolean hasNext = top.size() > (page + 1) * PER_PAGE;
        setItem(48, createItem(Material.ARROW, services.configManager.rawMessage("gui.top-previous")),
                e -> {
                    if (hasPrevious) {
                        page--;
                        refresh();
                    }
                });
        setItem(50, createItem(Material.ARROW, services.configManager.rawMessage("gui.top-next")),
                e -> {
                    if (hasNext) {
                        page++;
                        refresh();
                    }
                });

        int backSlot = services.configManager.backButtonSlot();
        setItem(backSlot >= 0 ? backSlot : 49, createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name")),
                e -> new RankMainMenu(player, services).open());
    }
}

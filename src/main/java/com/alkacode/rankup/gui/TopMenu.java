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

    private final RankUpServices services;
    private int page;

    public TopMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.rawMessage("gui.top-title"), 6, "rankup_top");
        this.services = services;
    }

    @Override
    public void render() {
        var layout = services.guiLayoutLoader.getLayout("rankup_top");
        fillBorder(createItem(services.configManager.material("rankup_top.filler", Material.GRAY_STAINED_GLASS_PANE), " "));

        List<Integer> contentSlots = layout.findSlots('0');
        List<PlayerRankData> top = services.playerDataManager.topPlayersSync((page + 1) * PER_PAGE);
        for (int i = 0; i < PER_PAGE && i < contentSlots.size(); i++) {
            int index = page * PER_PAGE + i;
            if (index >= top.size()) {
                break;
            }
            PlayerRankData data = top.get(index);
            OfflinePlayer target = Bukkit.getOfflinePlayer(data.uuid());
            Rank rank = services.rankManager.rankAt(data.rankIndex());
            String name = target.getName() != null ? target.getName() : "???";

            setItem(contentSlots.get(i), head(name, "<gold>#" + (index + 1) + " <white>" + name,
                    services.configManager.rawMessageList("gui.top-entry-lore").stream()
                            .map(line -> line.replace("<rank>", rank.displayName())
                                    .replace("<prestige>", String.valueOf(data.prestigeLevel())))
                            .toArray(String[]::new)));
        }

        boolean hasPrevious = page > 0;
        boolean hasNext = top.size() > (page + 1) * PER_PAGE;
        setItem(layout.firstSlot('A'), createItem(services.configManager.material("rankup_top.previous", Material.ARROW),
                services.configManager.rawMessage("gui.top-previous")),
                e -> {
                    if (hasPrevious) {
                        page--;
                        refresh();
                    }
                });
        setItem(layout.firstSlot('N'), createItem(services.configManager.material("rankup_top.next", Material.ARROW),
                services.configManager.rawMessage("gui.top-next")),
                e -> {
                    if (hasNext) {
                        page++;
                        refresh();
                    }
                });

        setItem(layout.firstSlot('V'), createItem(services.configManager.material("rankup_top.back", Material.ARROW),
                services.configManager.rawMessage("gui.back-name")),
                e -> new RankMainMenu(player, services).open());
    }
}

package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.manager.RankUpService;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.FeedbackUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

/** Confirmar/Cancelar centralizado pra ranquear - substitui o clique direto no rank da RankListMenu. */
public final class RankUpConfirmMenu extends BaseGui {

    private final RankUpServices services;

    public RankUpConfirmMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.rawMessage("gui.rankup-confirm-title"), 3, "rankup_confirm");
        this.services = services;
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        if (services.rankManager.isMaxRank(data.rankIndex())) {
            setItem(13, createItem(Material.BARRIER, services.configManager.rawMessage("rankup.max-rank")));
            setItem(22, createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name")),
                    e -> new RankMainMenu(player, services).open());
            return;
        }

        Rank next = services.rankManager.nextRank(data.rankIndex()).orElseThrow();
        setItem(13, createItem(next.icon().material(), next.displayName(),
                services.configManager.rawMessageList("gui.next-lore").stream()
                        .map(line -> line.replace("<cost>", services.requirementChecker.formatSummary(next.requirements())))
                        .toArray(String[]::new)));

        setItem(11, createItem(Material.LIME_WOOL, services.configManager.rawMessage("gui.prestige-confirm-yes")),
                e -> confirm());
        setItem(15, createItem(Material.RED_WOOL, services.configManager.rawMessage("gui.prestige-confirm-no")),
                e -> new RankMainMenu(player, services).open());
    }

    private void confirm() {
        RankUpService.Result result = services.rankUpService.attempt(player);
        switch (result.status()) {
            case REQUIREMENTS_UNAVAILABLE -> services.messages.send(player, "general.requirements-unavailable");
            case CANCELLED -> services.messages.send(player, "general.cancelled");
            case MAX_RANK -> services.messages.send(player, "rankup.max-rank");
            case REQUIREMENTS_NOT_MET -> services.messages.send(player, "rankup.insufficient-funds", Map.of(
                    "requirements", services.requirementChecker.formatUnmet(result.unmet()),
                    "rank", result.rank().displayName()));
            case SUCCESS -> {
                services.messages.send(player, "rankup.success", Map.of("rank", result.rank().displayName()));
                if (services.configManager.broadcastRankup()) {
                    Bukkit.getServer().sendMessage(services.messages.build("rankup.broadcast", Map.of(
                            "player", player.getName(),
                            "rank", result.rank().displayName())));
                }
                FeedbackUtil.play(player, services.configManager.feedbackSection("rankup"),
                        Map.of("rank", result.rank().displayName()));
            }
        }
        new RankMainMenu(player, services).open();
    }
}

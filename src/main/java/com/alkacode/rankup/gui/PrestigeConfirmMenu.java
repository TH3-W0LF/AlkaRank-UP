package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.manager.PrestigeManager;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.util.FeedbackUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;

/** Prestigiar reseta TODO o progresso de rank - nunca acontece direto no clique, sempre passa por essa confirmacao. */
public final class PrestigeConfirmMenu extends BaseGui {

    private final RankUpServices services;

    public PrestigeConfirmMenu(Player player, RankUpServices services) {
        super(services.plugin, player, title(services), rows(services), "rankup_prestige_confirm");
        this.services = services;
    }

    private static String title(RankUpServices services) {
        ConfigurationSection section = services.configManager.prestigeConfirmSection();
        return section != null ? section.getString("title", "Confirmar Prestigio?") : "Confirmar Prestigio?";
    }

    private static int rows(RankUpServices services) {
        ConfigurationSection section = services.configManager.prestigeConfirmSection();
        return Math.max(1, (section != null ? section.getInt("size", 27) : 27) / 9);
    }

    @Override
    public void render() {
        var layout = services.guiLayoutLoader.getLayout("rankup_prestige_confirm");
        fillBorder(createItem(services.configManager.material("rankup_prestige_confirm.filler", Material.GRAY_STAINED_GLASS_PANE), " "));

        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        double cost = services.prestigeManager.previewCost(player);
        int nextPrestige = data.prestigeLevel() + 1;
        double bonusPerLevel = services.configManager.sellBonusPerPrestige();

        int infoSlot = layout.firstSlot('I');
        if (infoSlot >= 0) {
            setItem(infoSlot, createItem(services.configManager.material("rankup_prestige_confirm.info", Material.NETHER_STAR),
                    services.configManager.rawMessage("gui.prestige-confirm-info-name"),
                    services.configManager.rawMessageList("gui.prestige-confirm-info-lore").stream()
                            .map(line -> line.replace("<cost>", String.valueOf(Math.round(cost)))
                                    .replace("<prestige>", String.valueOf(nextPrestige))
                                    .replace("<current_prestige>", String.valueOf(data.prestigeLevel()))
                                    .replace("<sell_bonus_current>", String.format("%.0f", bonusPerLevel * data.prestigeLevel() * 100))
                                    .replace("<sell_bonus_next>", String.format("%.0f", bonusPerLevel * nextPrestige * 100))
                                    .replace("<fly_status>", flyStatus(nextPrestige))
                                    .replace("<reward>", rewardSummary(nextPrestige)))
                            .toArray(String[]::new)));
        }

        int confirmSlot = layout.firstSlot('Y');
        if (confirmSlot >= 0) {
            setItem(confirmSlot, createItem(services.configManager.material("rankup_prestige_confirm.confirm", Material.LIME_WOOL),
                    services.configManager.rawMessage("gui.prestige-confirm-yes")),
                    e -> confirm());
        }

        int cancelSlot = layout.firstSlot('N');
        if (cancelSlot >= 0) {
            setItem(cancelSlot, createItem(services.configManager.material("rankup_prestige_confirm.cancel", Material.RED_WOOL),
                    services.configManager.rawMessage("gui.prestige-confirm-no")),
                    e -> new RankMainMenu(player, services).open());
        }
    }

    private String flyStatus(int nextPrestige) {
        int required = services.configManager.flyFromPrestige();
        if (required <= 0) {
            return services.configManager.rawMessage("gui.prestige-confirm-fly-disabled");
        }
        if (nextPrestige >= required) {
            return services.configManager.rawMessage("gui.prestige-confirm-fly-unlocked");
        }
        return services.configManager.rawMessage("gui.prestige-confirm-fly-locked").replace("<level>", String.valueOf(required));
    }

    private String rewardSummary(int nextPrestige) {
        boolean hasReward = !services.configManager.prestigeRewardItems(nextPrestige).isEmpty();
        return services.configManager.rawMessage(hasReward ? "gui.prestige-confirm-reward-yes" : "gui.prestige-confirm-reward-no");
    }

    private void confirm() {
        PrestigeManager.Result result = services.prestigeManager.attempt(player);
        switch (result.status()) {
            case ECONOMY_UNAVAILABLE -> services.messages.send(player, "general.economy-unavailable");
            case CANCELLED -> services.messages.send(player, "general.cancelled");
            case NOT_MAX_RANK -> services.messages.send(player, "prestige.not-max-rank");
            case INSUFFICIENT_FUNDS -> services.messages.send(player, "prestige.insufficient-funds", Map.of(
                    "cost", String.valueOf(Math.round(result.cost()))));
            case SUCCESS -> {
                services.messages.send(player, "prestige.success", Map.of("prestige", String.valueOf(result.prestigeLevel())));
                if (services.configManager.broadcastPrestige()) {
                    Bukkit.getServer().sendMessage(services.messages.build("prestige.broadcast", Map.of(
                            "player", player.getName(),
                            "prestige", String.valueOf(result.prestigeLevel()))));
                }
                FeedbackUtil.play(player, services.configManager.feedbackSection("prestige"),
                        Map.of("prestige", String.valueOf(result.prestigeLevel())));
            }
        }
        new RankMainMenu(player, services).open();
    }
}

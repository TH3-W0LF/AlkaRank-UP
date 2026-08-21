package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.manager.RankUpService;
import com.alkacode.rankup.manager.RequirementChecker;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.FeedbackUtil;
import com.alkacode.rankup.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Confirmar/Cancelar centralizado pra ranquear - substitui o clique direto no rank da RankListMenu. */
public final class RankUpConfirmMenu extends BaseGui {

    private final RankUpServices services;

    public RankUpConfirmMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.rawMessage("gui.rankup-confirm-title"), 6, "rankup_confirm");
        this.services = services;
    }

    @Override
    public void render() {
        String[] layout = services.guiLayoutLoader.getLayout("rankup_confirm").layout();
        Map<Character, ItemStack> icons = new HashMap<>();
        Map<Character, Consumer<InventoryClickEvent>> clicks = new HashMap<>();
        icons.put('G', createItem(services.configManager.material("rankup_confirm.filler", Material.GRAY_STAINED_GLASS_PANE), " "));
        icons.put('V', createItem(services.configManager.material("rankup_confirm.back", Material.ARROW),
                services.configManager.rawMessage("gui.back-name"), lore("gui.back-lore")));
        clicks.put('V', e -> new RankMainMenu(player, services).open());

        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        if (services.rankManager.isMaxRank(data.rankIndex())) {
            icons.put('I', createItem(services.configManager.material("rankup_confirm.locked", Material.BARRIER),
                    services.configManager.rawMessage("rankup.max-rank"), lore("gui.rankup-max-rank-lore")));
            layout(layout, icons, clicks);
            return;
        }

        Rank current = services.rankManager.rankAt(data.rankIndex());
        Rank next = services.rankManager.nextRank(data.rankIndex()).orElseThrow();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<current_rank>", current.displayName());
        placeholders.put("<next_rank>", next.displayName());
        placeholders.put("<cost>", services.requirementChecker.formatSummary(next.requirements()));
        placeholders.put("<prestige>", String.valueOf(data.prestigeLevel()));
        placeholders.put("<progress>", RankListMenu.progressBar(services.requirementChecker.progressPercent(player, next.requirements())));

        List<RequirementChecker.Unmet> unmet = services.requirementChecker.checkUnmet(player, next.requirements());
        placeholders.put("<requirements>", unmet.isEmpty()
                ? services.configManager.rawMessage("gui.rankup-confirm-requirements-met")
                : services.requirementChecker.formatUnmet(unmet));

        ItemStack info = com.alkacode.rankup.util.IconFactory.build(next.icon(), null, List.of());
        ItemMeta meta = info.getItemMeta();
        meta.displayName(TextUtil.parse(
                services.configManager.rawMessage("gui.rankup-confirm-info-name").replace("<next_rank>", next.displayName())));
        meta.lore(RankListMenu.replacePlaceholders(services.configManager.rawMessageList("gui.rankup-confirm-info-lore"), placeholders));
        info.setItemMeta(meta);
        icons.put('I', info);

        ItemStack confirmItem = new ItemStack(services.configManager.material("rankup_confirm.confirm", Material.LIME_WOOL));
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(TextUtil.parse(services.configManager.rawMessage("gui.rankup-confirm-yes-name")));
        confirmMeta.lore(RankListMenu.replacePlaceholders(services.configManager.rawMessageList("gui.rankup-confirm-yes-lore"), placeholders));
        confirmItem.setItemMeta(confirmMeta);
        icons.put('Y', confirmItem);
        clicks.put('Y', e -> confirm());

        icons.put('N', createItem(services.configManager.material("rankup_confirm.cancel", Material.RED_WOOL),
                services.configManager.rawMessage("gui.rankup-confirm-no-name"), lore("gui.rankup-confirm-no-lore")));
        clicks.put('N', e -> new RankMainMenu(player, services).open());

        layout(layout, icons, clicks);
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

    private String[] lore(String path) {
        return services.configManager.rawMessageList(path).toArray(new String[0]);
    }
}

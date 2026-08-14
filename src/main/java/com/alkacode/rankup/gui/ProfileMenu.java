package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.hook.TimeHook;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Resumo somente-leitura: rank atual, prestigio, tempo online e total de cabecas bancadas. */
public final class ProfileMenu extends BaseGui {

    private final RankUpServices services;

    public ProfileMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.rawMessage("gui.profile-title"), 3, "rankup_profile");
        this.services = services;
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        Rank current = services.rankManager.rankAt(data.rankIndex());

        TimeHook timeHook = services.timeHookSupplier.get();
        long onlineSeconds = timeHook != null ? timeHook.getOnlineSeconds(player.getUniqueId()) : -1;
        String onlineText = onlineSeconds >= 0 ? TimeUtil.formatDuration(onlineSeconds)
                : services.configManager.rawMessage("gui.profile-time-unavailable");

        int headsTotal = services.headsManager.all(player.getUniqueId()).values().stream()
                .mapToInt(Integer::intValue).sum();

        setItem(13, createItem(current.icon().material(), services.configManager.rawMessage("gui.profile-info-name"),
                services.configManager.rawMessageList("gui.profile-info-lore").stream()
                        .map(line -> line
                                .replace("<rank>", current.displayName())
                                .replace("<prestige>", String.valueOf(data.prestigeLevel()))
                                .replace("<time>", onlineText)
                                .replace("<heads>", String.valueOf(headsTotal)))
                        .toArray(String[]::new)));

        // Slot fixo (nao usa o back_button.slot global) - esse menu tem 27 slots (3 linhas),
        // o slot global (27) e calibrado pros menus de kit de 36 slots e ficaria fora do range aqui.
        setItem(22, createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name")),
                e -> new RankMainMenu(player, services).open());
    }
}

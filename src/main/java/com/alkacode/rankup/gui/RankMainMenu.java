package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.model.PlayerRankData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Hub principal do /rankup - so navegacao, nenhuma transacao acontece aqui. */
public final class RankMainMenu extends BaseGui {

    private final RankUpServices services;

    public RankMainMenu(Player player, RankUpServices services) {
        super(services.plugin, player, services.configManager.rawMessage("gui.main-title"), 6, "rankup_main");
        this.services = services;
    }

    @Override
    public void render() {
        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        boolean maxRank = services.rankManager.isMaxRank(data.rankIndex());

        Map<Character, ItemStack> icons = new HashMap<>();
        Map<Character, Consumer<InventoryClickEvent>> clicks = new HashMap<>();

        icons.put('G', createItem(services.configManager.material("rankup_main.filler", Material.GRAY_STAINED_GLASS_PANE), " "));

        icons.put('H', createItem(services.configManager.material("rankup_main.heads", Material.SKELETON_SKULL),
                services.configManager.rawMessage("gui.main-heads-name"), lore("gui.main-heads-lore")));
        clicks.put('H', e -> new HeadsMenu(player, services).open());

        icons.put('R', createItem(services.configManager.material("rankup_main.ranks", Material.BOOK),
                services.configManager.rawMessage("gui.main-ranks-name"), lore("gui.main-ranks-lore")));
        clicks.put('R', e -> new RankListMenu(player, services).open());

        icons.put('P', createItem(services.configManager.material("rankup_main.profile", Material.PLAYER_HEAD),
                services.configManager.rawMessage("gui.main-profile-name"), lore("gui.main-profile-lore")));
        clicks.put('P', e -> new ProfileMenu(player, services).open());

        icons.put('T', createItem(services.configManager.material("rankup_main.top", Material.GOLD_BLOCK),
                services.configManager.rawMessage("gui.main-top-name"), lore("gui.main-top-lore")));
        clicks.put('T', e -> new TopMenu(player, services).open());

        icons.put('U', createItem(services.configManager.material("rankup_main.rankup", Material.EXPERIENCE_BOTTLE),
                services.configManager.rawMessage("gui.main-rankup-name"), lore("gui.main-rankup-lore")));
        clicks.put('U', e -> new RankUpConfirmMenu(player, services).open());

        // Icone de prestigio muda conforme elegibilidade (max rank ou nao) - permanece dinamico em Java.
        icons.put('N', createItem(maxRank ? Material.NETHER_STAR : Material.GRAY_DYE,
                services.configManager.rawMessage("gui.main-prestige-name"), lore("gui.main-prestige-lore")));
        clicks.put('N', e -> {
            switch (services.prestigeManager.checkEligibility(player)) {
                case ECONOMY_UNAVAILABLE -> services.messages.send(player, "general.economy-unavailable");
                case NOT_MAX_RANK -> services.messages.send(player, "prestige.not-max-rank");
                default -> new PrestigeConfirmMenu(player, services).open();
            }
        });

        layout(services.guiLayoutLoader.getLayout("rankup_main").layout(), icons, clicks);
    }

    private String[] lore(String path) {
        return services.configManager.rawMessageList(path).toArray(new String[0]);
    }
}

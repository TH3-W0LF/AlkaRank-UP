package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.model.Kit;
import com.alkacode.rankup.model.KitType;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.TextUtil;
import com.alkacode.rankup.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Optional;

/** Kits do rank ATUAL do jogador (rankId fixado na abertura) - so ranks anteriores nao ficam mais acessiveis. */
public final class KitMenu extends BaseGui {

    private final RankUpServices services;
    private final String rankId;

    public KitMenu(Player player, RankUpServices services, String rankId) {
        super(services.plugin, player, services.configManager.kitsMenuSection().getString("title", "<dark_gray>Kits</dark_gray>")
                        .replace("<rank>", rankName(services, rankId)),
                Math.max(1, services.configManager.kitsMenuSection().getInt("size", 36) / 9), "rankup_kits");
        this.services = services;
        this.rankId = rankId;
    }

    private static String rankName(RankUpServices services, String rankId) {
        return services.rankManager.byId(rankId).map(Rank::displayName).orElse(rankId);
    }

    @Override
    public void render() {
        Optional<Rank> rankOpt = services.rankManager.byId(rankId);
        if (rankOpt.isEmpty()) {
            return;
        }
        Rank rank = rankOpt.get();
        ConfigurationSection gui = services.configManager.kitsMenuSection();

        for (KitType type : KitType.values()) {
            int slot = gui.getInt(type.name().toLowerCase() + "-slot", -1);
            if (slot < 0) {
                continue;
            }
            rank.kit(type).ifPresent(kit -> setItem(slot, buildKitItem(rank, kit),
                    e -> new KitPreviewMenu(player, services, rank.id(), type).open()));
        }

        int backSlot = services.configManager.backButtonSlot();
        if (backSlot >= 0) {
            setItem(backSlot, createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name")),
                    e -> new RankListMenu(player, services).open());
        }

        if (services.configManager.fillEmptySlotsEnabled()) {
            var icon = services.configManager.fillEmptySlotsIcon();
            fill(createItem(icon.material(), icon.displayName() != null ? icon.displayName() : " "));
        }
    }

    private ItemStack buildKitItem(Rank rank, Kit kit) {
        long lastClaimed = services.kitCooldownManager.lastClaimed(player.getUniqueId(), rank.id(), kit.type());
        long now = System.currentTimeMillis() / 1000;
        long elapsed = now - lastClaimed;
        boolean available = lastClaimed == 0 || elapsed >= kit.cooldownSeconds();

        ItemStack item = new ItemStack(available ? kit.iconAvailable().material() : kit.iconCooldown().material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse(kit.name()));
        if (available) {
            meta.lore(TextUtil.parseList(services.configManager.rawMessageList("gui.kit-available-lore"), Map.of()));
        } else {
            long remaining = kit.cooldownSeconds() - elapsed;
            meta.lore(TextUtil.parseList(services.configManager.rawMessageList("gui.kit-cooldown-lore"),
                    Map.of("time", TimeUtil.formatDuration(remaining))));
        }
        item.setItemMeta(meta);
        return item;
    }
}

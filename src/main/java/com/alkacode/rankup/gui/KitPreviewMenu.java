package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.event.KitClaimEvent;
import com.alkacode.rankup.model.Kit;
import com.alkacode.rankup.model.KitItem;
import com.alkacode.rankup.model.KitType;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.KitItemFactory;
import com.alkacode.rankup.util.TextUtil;
import com.alkacode.rankup.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class KitPreviewMenu extends BaseGui {

    private final RankUpServices services;
    private final String rankId;
    private final KitType kitType;

    public KitPreviewMenu(Player player, RankUpServices services, String rankId, KitType kitType) {
        super(services.plugin, player, services.configManager.kitPreviewSection().getString("title", "<dark_gray>Preview</dark_gray>")
                        .replace("<kit>", kitName(services, rankId, kitType)),
                Math.max(1, services.configManager.kitPreviewSection().getInt("size", 36) / 9), "rankup_kit_preview");
        this.services = services;
        this.rankId = rankId;
        this.kitType = kitType;
    }

    private static String kitName(RankUpServices services, String rankId, KitType kitType) {
        return services.rankManager.byId(rankId).flatMap(rank -> rank.kit(kitType)).map(Kit::name).orElse(kitType.name());
    }

    private Optional<Kit> kit() {
        return services.rankManager.byId(rankId).flatMap(rank -> rank.kit(kitType));
    }

    @Override
    public void render() {
        Optional<Kit> kitOpt = kit();
        if (kitOpt.isEmpty()) {
            return;
        }
        Kit kit = kitOpt.get();
        ConfigurationSection gui = services.configManager.kitPreviewSection();

        List<Integer> itemSlots = gui.getIntegerList("item-slots");
        List<KitItem> items = kit.items();
        for (int i = 0; i < items.size() && i < itemSlots.size(); i++) {
            setItem(itemSlots.get(i), KitItemFactory.build(items.get(i)));
        }

        int actionSlot = services.configManager.claimButtonSlot();
        if (actionSlot >= 0) {
            setItem(actionSlot, buildActionItem(kit), e -> handleClaim(kit));
        }

        int backSlot = services.configManager.backButtonSlot();
        if (backSlot >= 0) {
            setItem(backSlot, createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name")),
                    e -> new KitMenu(player, services, rankId).open());
        }

        if (services.configManager.fillEmptySlotsEnabled()) {
            var icon = services.configManager.fillEmptySlotsIcon();
            fill(createItem(icon.material(), icon.displayName() != null ? icon.displayName() : " "));
        }
    }

    private ItemStack buildActionItem(Kit kit) {
        long lastClaimed = services.kitCooldownManager.lastClaimed(player.getUniqueId(), rankId, kit.type());
        long now = System.currentTimeMillis() / 1000;
        long elapsed = now - lastClaimed;
        boolean available = lastClaimed == 0 || elapsed >= kit.cooldownSeconds();

        var icon = available ? services.configManager.claimButtonIcon() : services.configManager.cooldownButtonIcon();
        ItemStack item = new ItemStack(icon.material());
        ItemMeta meta = item.getItemMeta();
        if (available) {
            meta.lore(TextUtil.parseList(services.configManager.rawMessageList("gui.kit-claim-lore"), Map.of()));
        } else {
            long remaining = kit.cooldownSeconds() - elapsed;
            meta.lore(TextUtil.parseList(services.configManager.rawMessageList("gui.kit-cooldown-lore"),
                    Map.of("time", TimeUtil.formatDuration(remaining))));
        }
        item.setItemMeta(meta);
        return item;
    }

    private void handleClaim(Kit kit) {
        Rank rank = services.rankManager.byId(rankId).orElse(null);
        if (rank == null) {
            return;
        }
        long lastClaimed = services.kitCooldownManager.lastClaimed(player.getUniqueId(), rankId, kit.type());
        long now = System.currentTimeMillis() / 1000;
        long elapsed = now - lastClaimed;

        if (lastClaimed != 0 && elapsed < kit.cooldownSeconds()) {
            long remaining = kit.cooldownSeconds() - elapsed;
            services.messages.send(player, "kits.on-cooldown", Map.of("time", TimeUtil.formatDuration(remaining)));
            return;
        }

        KitClaimEvent claimEvent = new KitClaimEvent(player, rank, kit);
        Bukkit.getPluginManager().callEvent(claimEvent);
        if (claimEvent.isCancelled()) {
            return;
        }

        services.kitCooldownManager.markClaimed(player.getUniqueId(), rankId, kit.type(), now);
        for (KitItem kitItem : kit.items()) {
            ItemStack stack = KitItemFactory.build(kitItem);
            for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        services.messages.send(player, "kits.claimed", Map.of("kit", kit.name()));

        if (services.configManager.closeGuiOnClaim()) {
            player.closeInventory();
        } else {
            refresh();
        }
    }
}

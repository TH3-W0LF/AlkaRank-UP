package com.alkacode.rankup.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.rankup.hook.TimeHook;
import com.alkacode.rankup.model.IconConfig;
import com.alkacode.rankup.model.Kit;
import com.alkacode.rankup.model.KitType;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.IconFactory;
import com.alkacode.rankup.util.TextUtil;
import com.alkacode.rankup.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;

/** Resumo do progresso do jogador - varios blocos de info (rank atual/proximo, prestigio, tempo, cabecas, kits). */
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
        boolean maxRank = services.rankManager.isMaxRank(data.rankIndex());

        setItem(10, item(current.icon(), "gui.profile-rank-name", "gui.profile-rank-lore",
                Map.of("<rank>", current.displayName())));

        if (!maxRank) {
            Rank next = services.rankManager.nextRank(data.rankIndex()).orElseThrow();
            Map<String, String> ph = new HashMap<>();
            ph.put("<next_rank>", next.displayName());
            ph.put("<cost>", services.requirementChecker.formatSummary(next.requirements()));
            ph.put("<progress>", RankListMenu.progressBar(services.requirementChecker.progressPercent(player, next.requirements())));
            setItem(11, item(next.icon(), "gui.profile-next-name", "gui.profile-next-lore", ph));
        } else {
            setItem(11, item(Material.NETHER_STAR, "gui.profile-maxrank-name", "gui.profile-maxrank-lore", Map.of()));
        }

        setItem(12, item(Material.END_CRYSTAL, "gui.profile-prestige-name", "gui.profile-prestige-lore",
                Map.of("<prestige>", String.valueOf(data.prestigeLevel()))));

        setItem(13, playerHead());

        TimeHook timeHook = services.timeHookSupplier.get();
        long onlineSeconds = timeHook != null ? timeHook.getOnlineSeconds(player.getUniqueId()) : -1;
        String onlineText = onlineSeconds >= 0 ? TimeUtil.formatDuration(onlineSeconds)
                : services.configManager.rawMessage("gui.profile-time-unavailable");
        setItem(14, item(Material.CLOCK, "gui.profile-time-name", "gui.profile-time-lore", Map.of("<time>", onlineText)));

        int headsTotal = services.headsManager.all(player.getUniqueId()).values().stream()
                .mapToInt(Integer::intValue).sum();
        setItem(15, item(Material.SKELETON_SKULL, "gui.profile-heads-name", "gui.profile-heads-lore",
                Map.of("<heads>", String.valueOf(headsTotal))));

        setItem(16, item(Material.CHEST, "gui.profile-kits-name", "gui.profile-kits-lore",
                Map.of("<kits>", String.valueOf(countAvailableKits(current)))));

        // Slot fixo (nao usa o back_button.slot global) - esse menu tem 27 slots (3 linhas),
        // o slot global (27) e calibrado pros menus de kit de 36 slots e ficaria fora do range aqui.
        setItem(22, createItem(Material.ARROW, services.configManager.rawMessage("gui.back-name")),
                e -> new RankMainMenu(player, services).open());
    }

    /** Item com nome/lore vindo do messages.yml + placeholders substituidos ANTES do MiniMessage
     * (createItem() do BaseGui nao suporta placeholder - so texto cru). */
    private ItemStack item(Material material, String nameKey, String loreKey, Map<String, String> placeholders) {
        ItemStack stack = new ItemStack(material);
        return applyNameLore(stack, nameKey, loreKey, placeholders);
    }

    /** Mesma coisa, mas resolvendo o icone base via IconFactory (respeita ItemsAdder/textura
     * Base64/material configurados no rank - ver IconConfig). */
    private ItemStack item(IconConfig icon, String nameKey, String loreKey, Map<String, String> placeholders) {
        ItemStack stack = IconFactory.build(icon, null, java.util.List.of());
        return applyNameLore(stack, nameKey, loreKey, placeholders);
    }

    private ItemStack applyNameLore(ItemStack stack, String nameKey, String loreKey, Map<String, String> placeholders) {
        String name = services.configManager.rawMessage(nameKey);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            name = name.replace(entry.getKey(), entry.getValue());
        }
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(TextUtil.parse(name));
        meta.lore(RankListMenu.replacePlaceholders(services.configManager.rawMessageList(loreKey), placeholders));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack playerHead() {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(TextUtil.parse(services.configManager.rawMessage("gui.profile-player-name")
                .replace("<player>", player.getName())));
        meta.lore(RankListMenu.replacePlaceholders(services.configManager.rawMessageList("gui.profile-player-lore"),
                Map.of("<player>", player.getName())));
        skull.setItemMeta(meta);
        return skull;
    }

    /** Quantos kits do rank atual o jogador pode resgatar AGORA (fora de cooldown). */
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
}

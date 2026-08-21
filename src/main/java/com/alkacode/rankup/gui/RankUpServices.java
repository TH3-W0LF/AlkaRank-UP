package com.alkacode.rankup.gui;

import com.alkacode.rankup.config.ConfigManager;
import com.alkacode.rankup.config.Messages;
import com.alkacode.rankup.gui.layout.GuiLayoutLoader;
import com.alkacode.rankup.database.PrestigeRewardsRepository;
import com.alkacode.rankup.manager.HeadsManager;
import com.alkacode.rankup.manager.KitCooldownManager;
import com.alkacode.rankup.manager.PlayerDataManager;
import com.alkacode.rankup.manager.PrestigeManager;
import com.alkacode.rankup.manager.RankManager;
import com.alkacode.rankup.manager.RankUpService;
import com.alkacode.rankup.manager.RequirementChecker;
import com.alkacode.rankup.hook.TimeHook;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

/** Bag de dependencias compartilhadas por todas as GUIs do AlkaRankUp - evita repetir 10 parametros em cada construtor. */
public final class RankUpServices {

    public final JavaPlugin plugin;
    public final RankManager rankManager;
    public final PlayerDataManager playerDataManager;
    public final KitCooldownManager kitCooldownManager;
    public final HeadsManager headsManager;
    public final RankUpService rankUpService;
    public final PrestigeManager prestigeManager;
    public final RequirementChecker requirementChecker;
    public final ConfigManager configManager;
    public final Messages messages;
    public final NamespacedKey headIdKey;
    public final Supplier<TimeHook> timeHookSupplier;
    public final PrestigeRewardsRepository prestigeRewardsRepository;
    public final com.alkacode.core.util.PermissionNamesStore permissionNames;
    public final GuiLayoutLoader guiLayoutLoader;

    public RankUpServices(JavaPlugin plugin, RankManager rankManager, PlayerDataManager playerDataManager,
                           KitCooldownManager kitCooldownManager, HeadsManager headsManager, RankUpService rankUpService,
                           PrestigeManager prestigeManager, RequirementChecker requirementChecker,
                           ConfigManager configManager, Messages messages, NamespacedKey headIdKey,
                           Supplier<TimeHook> timeHookSupplier, PrestigeRewardsRepository prestigeRewardsRepository,
                           com.alkacode.core.util.PermissionNamesStore permissionNames, GuiLayoutLoader guiLayoutLoader) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        this.playerDataManager = playerDataManager;
        this.kitCooldownManager = kitCooldownManager;
        this.headsManager = headsManager;
        this.rankUpService = rankUpService;
        this.prestigeManager = prestigeManager;
        this.requirementChecker = requirementChecker;
        this.configManager = configManager;
        this.messages = messages;
        this.headIdKey = headIdKey;
        this.timeHookSupplier = timeHookSupplier;
        this.prestigeRewardsRepository = prestigeRewardsRepository;
        this.permissionNames = permissionNames;
        this.guiLayoutLoader = guiLayoutLoader;
    }

    /** Linhas de lore "permissoes deste rank" ja resolvidas com nomes amigaveis - vazio se
     * o rank nao tem lpGroup configurado. Ver com.alkacode.rankup.model.Rank#lpGroup(). */
    public java.util.List<String> permissionLoreLines(String lpGroup) {
        if (lpGroup == null || lpGroup.isBlank()) {
            return java.util.List.of();
        }
        java.util.List<String> keys = com.alkacode.core.hooks.LuckPermsHook.getGroupPermissionKeys(lpGroup);
        return com.alkacode.core.util.PermissionLoreUtil.expand(keys, configManager.permissionLoreLineFormat(),
                permissionNames::lookup, permissionNames::registerUnknown);
    }
}

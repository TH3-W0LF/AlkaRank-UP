package com.alkacode.rankup.command;

import com.alkacode.rankup.gui.PrestigeConfirmMenu;
import com.alkacode.rankup.gui.PrestigeRewardsMenu;
import com.alkacode.rankup.gui.RankUpServices;
import com.alkacode.rankup.manager.PrestigeManager;
import com.alkacode.rankup.model.PlayerRankData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class PrestigeCommand implements CommandExecutor {

    private final RankUpServices services;

    public PrestigeCommand(RankUpServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            services.messages.send(sender, "general.player-only");
            return true;
        }

        if (!player.hasPermission("alkarankup.use")) {
            services.messages.send(player, "general.no-permission");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("rewards")) {
            new PrestigeRewardsMenu(player, services).open();
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("fly")) {
            handleFly(player);
            return true;
        }

        PrestigeManager.Status status = services.prestigeManager.checkEligibility(player);
        switch (status) {
            case ECONOMY_UNAVAILABLE -> services.messages.send(player, "general.economy-unavailable");
            case NOT_MAX_RANK -> services.messages.send(player, "prestige.not-max-rank");
            default -> new PrestigeConfirmMenu(player, services).open();
        }
        return true;
    }

    /** Beneficio de prestigio: voo liberado a partir de perks.fly-from-prestige. Toggle simples, nao persiste entre sessoes. */
    private void handleFly(Player player) {
        int required = services.configManager.flyFromPrestige();
        if (required <= 0) {
            services.messages.send(player, "prestige.fly-disabled-globally");
            return;
        }

        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        if (data.prestigeLevel() < required) {
            services.messages.send(player, "prestige.fly-locked", Map.of("level", String.valueOf(required)));
            return;
        }

        boolean nowFlying = !player.getAllowFlight();
        player.setAllowFlight(nowFlying);
        if (!nowFlying) {
            player.setFlying(false);
        }
        services.messages.send(player, nowFlying ? "prestige.fly-enabled" : "prestige.fly-disabled");
    }
}

package com.alkacode.rankup.command;

import com.alkacode.rankup.gui.KitMenu;
import com.alkacode.rankup.gui.RankMainMenu;
import com.alkacode.rankup.gui.RankUpServices;
import com.alkacode.rankup.manager.RankManager;
import com.alkacode.rankup.model.PlayerRankData;
import com.alkacode.rankup.model.Rank;
import com.alkacode.rankup.util.LuckPermsSync;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * A experiencia de rankup e 100% visual: este comando so abre menus. A transacao de
 * compra em si so acontece dentro da {@code RankUpConfirmMenu} ao confirmar.
 */
public final class RankUpCommand implements CommandExecutor {

    private final RankUpServices services;

    public RankUpCommand(RankUpServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            handleAdmin(sender, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            services.messages.send(sender, "general.player-only");
            return true;
        }

        if (!player.hasPermission("alkarankup.use")) {
            services.messages.send(player, "general.no-permission");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("kits")) {
            handleKits(player, args);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
            handleTop(player, args);
            return true;
        }

        // Sem argumentos (ou qualquer outro que nao seja um subcomando reconhecido,
        // ex: "gui") sempre abre o Menu Principal.
        new RankMainMenu(player, services).open();
        return true;
    }

    private void handleTop(Player player, String[] args) {
        int limit = 10;
        if (args.length >= 2) {
            try {
                limit = Math.max(1, Math.min(50, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
                // mantem o default de 10 se o argumento nao for um numero valido.
            }
        }

        List<PlayerRankData> top = services.playerDataManager.topPlayersSync(limit);
        if (top.isEmpty()) {
            services.messages.send(player, "top.empty");
            return;
        }

        services.messages.send(player, "top.header");
        int position = 1;
        for (PlayerRankData data : top) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(data.uuid());
            Rank rank = services.rankManager.rankAt(data.rankIndex());
            services.messages.send(player, "top.line", Map.of(
                    "pos", String.valueOf(position++),
                    "name", target.getName() != null ? target.getName() : "???",
                    "rank", rank.displayName(),
                    "prestige", String.valueOf(data.prestigeLevel())));
        }
    }

    private void handleKits(Player player, String[] args) {
        if (args.length < 2) {
            services.messages.send(player, "kits.usage");
            return;
        }

        Rank rank = services.rankManager.byId(args[1]).orElse(null);
        if (rank == null) {
            services.messages.send(player, "general.unknown-rank", Map.of("value", args[1]));
            return;
        }

        PlayerRankData data = services.playerDataManager.get(player.getUniqueId());
        if (rank.index() != data.rankIndex()) {
            services.messages.send(player, "kits.locked-rank");
            return;
        }

        new KitMenu(player, services, rank.id()).open();
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkarankup.admin")) {
            services.messages.send(sender, "general.no-permission");
            return;
        }
        RankManager rankManager = services.rankManager;
        if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
            services.configManager.reload();
            rankManager.reload(services.configManager.ranks());
            services.messages.send(sender, "admin.reload-success");

            int slotCount = services.configManager.guiSection().getIntegerList("rank-slots").size();
            int rankCount = rankManager.ranks().size();
            if (slotCount != rankCount) {
                services.messages.send(sender, "admin.reload-slot-mismatch", Map.of(
                        "slots", String.valueOf(slotCount), "ranks", String.valueOf(rankCount)));
            }
            return;
        }
        if (args.length < 4) {
            services.messages.send(sender, "admin.usage");
            return;
        }

        String action = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            services.messages.send(sender, "general.unknown-player", Map.of("name", args[2]));
            return;
        }

        PlayerRankData data = services.playerDataManager.get(target.getUniqueId());

        switch (action) {
            case "setrank" -> {
                int rankIndex = resolveRankIndex(args[3]);
                if (rankIndex < 0) {
                    services.messages.send(sender, "admin.invalid-rank", Map.of("value", args[3]));
                    return;
                }
                Rank previous = rankManager.rankAt(data.rankIndex());
                Rank updated = rankManager.rankAt(rankIndex);
                data.setRankIndex(rankIndex);
                services.playerDataManager.persist(data);
                if (services.configManager.luckPermsSyncEnabled() && target.getName() != null) {
                    LuckPermsSync.swapGroup(target.getName(), previous.lpGroup(), updated.lpGroup());
                }
                services.messages.send(sender, "admin.set-rank-success", Map.of("name", args[2], "value", args[3]));
            }
            case "setprestige" -> {
                try {
                    int level = Integer.parseInt(args[3]);
                    data.setPrestigeLevel(level);
                    services.playerDataManager.persist(data);
                    services.messages.send(sender, "admin.set-prestige-success", Map.of("name", args[2], "value", args[3]));
                } catch (NumberFormatException e) {
                    services.messages.send(sender, "admin.invalid-number", Map.of("value", args[3]));
                }
            }
            default -> services.messages.send(sender, "admin.usage");
        }
    }

    private int resolveRankIndex(String value) {
        for (Rank rank : services.rankManager.ranks()) {
            if (rank.id().equalsIgnoreCase(value)) {
                return rank.index();
            }
        }
        try {
            int index = Integer.parseInt(value);
            return index >= 0 && index <= services.rankManager.maxIndex() ? index : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

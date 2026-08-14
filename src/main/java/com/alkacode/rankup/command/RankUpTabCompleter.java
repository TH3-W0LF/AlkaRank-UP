package com.alkacode.rankup.command;

import com.alkacode.rankup.manager.RankManager;
import com.alkacode.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class RankUpTabCompleter implements TabCompleter {

    private static final List<String> ADMIN_ACTIONS = List.of("setrank", "setprestige", "reload");
    private static final List<String> TOP_SUGGESTIONS = List.of("10", "25", "50");

    private final RankManager rankManager;

    public RankUpTabCompleter(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String alias, @NotNull String[] args) {
        List<String> options = new ArrayList<>();

        if (args.length == 1) {
            options.add("kits");
            options.add("top");
            if (sender.hasPermission("alkarankup.admin")) {
                options.add("admin");
            }
            return filter(options, args[0]);
        }

        if (args[0].equalsIgnoreCase("kits") && args.length == 2) {
            return filter(rankIds(), args[1]);
        }

        if (args[0].equalsIgnoreCase("top") && args.length == 2) {
            return filter(TOP_SUGGESTIONS, args[1]);
        }

        if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("alkarankup.admin")) {
            return adminTabComplete(args);
        }

        return List.of();
    }

    private List<String> adminTabComplete(String[] args) {
        if (args.length == 2) {
            return filter(ADMIN_ACTIONS, args[1]);
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("setrank") || args[1].equalsIgnoreCase("setprestige"))) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return filter(names, args[2]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("setrank")) {
            return filter(rankIds(), args[3]);
        }
        return List.of();
    }

    private List<String> rankIds() {
        return rankManager.ranks().stream().map(Rank::id).collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(option -> option.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}

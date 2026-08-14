package com.alkacode.rankup.util;

import org.bukkit.Bukkit;

/**
 * Troca o grupo do LuckPerms ao ranquear - remove o grupo do rank ANTERIOR, adiciona o
 * do novo (nunca acumula, ver Rank#lpGroup). Via comando de console (`lp user ... parent
 * remove/add`), NAO a API Java do LuckPerms direto - mesmo padrao ja usado pelo AlkaVips
 * (vips.yml -> group-command), evita reflexao profunda em cima do Node/NodeBuilder do LP
 * (API bem mais complexa que os getters simples que os outros hooks usam) pra um ganho
 * que dois comandos de console ja resolvem igual.
 */
public final class LuckPermsSync {

    private LuckPermsSync() {
    }

    public static boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }

    /** Nao faz nada se o LuckPerms nao estiver instalado, ou se o grupo antigo e o novo forem o mesmo. */
    public static void swapGroup(String playerName, String oldGroup, String newGroup) {
        if (!isPresent() || oldGroup == null || oldGroup.equalsIgnoreCase(newGroup)) {
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " parent remove " + oldGroup);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + playerName + " parent add " + newGroup);
    }
}

package com.alkacode.rankup.economy;

import com.alkacode.rankup.hook.EconomyHook;
import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Ponte para o AlkaEconomy - moeda resolvida por String (config-driven), NUNCA
 * hardcoded (mesmo padrao de todo Alka* que mexe com dinheiro - ver AlkaShop/AlkaDrop).
 * Cada rank pode ter sua propria moeda (ver Rank#currency/ConfigManager), com
 * "economy.currency" como fallback global.
 *
 * AlkaEconomy agora e softdepend - o hook so fica pronto 1 tick depois do onEnable (ver
 * AlkaRankUpPlugin), entao {@link #isAvailable()} pode retornar false por um instante
 * mesmo com o AlkaEconomy instalado, e permanece false pra sempre se ele nao estiver.
 * Guarda um Supplier (nunca o hook resolvido direto) - o valor pode mudar de null pra
 * nao-null entre o onEnable e o primeiro uso real.
 */
public final class EconomyService {

    private final Supplier<EconomyHook> hookSupplier;

    public EconomyService(Supplier<EconomyHook> hookSupplier) {
        this.hookSupplier = hookSupplier;
    }

    public boolean isAvailable() {
        return hookSupplier.get() != null;
    }

    public boolean has(OfflinePlayer player, String currency, double amount) {
        EconomyHook hook = hookSupplier.get();
        return hook != null && hook.has(player.getUniqueId(), resolve(hook, currency), amount);
    }

    public double getBalance(OfflinePlayer player, String currency) {
        EconomyHook hook = hookSupplier.get();
        return hook != null ? hook.getBalance(player.getUniqueId(), resolve(hook, currency)) : 0.0;
    }

    public boolean withdraw(OfflinePlayer player, String currency, double amount) {
        EconomyHook hook = hookSupplier.get();
        if (hook == null) {
            return false;
        }
        String id = resolve(hook, currency);
        if (!hook.has(player.getUniqueId(), id, amount)) {
            return false;
        }
        hook.removeBalance(player.getUniqueId(), id, amount);
        return true;
    }

    public String format(double amount) {
        EconomyHook hook = hookSupplier.get();
        return hook != null ? hook.formatValue(amount) : String.valueOf(amount);
    }

    /** Cai pra "gold" se a moeda configurada nao existir mais no AlkaEconomy (ex: id digitado errado no config.yml). */
    private String resolve(EconomyHook hook, String currency) {
        String id = currency == null ? "" : currency.toLowerCase(Locale.ROOT);
        return hook.isValidCurrency(id) ? id : "gold";
    }
}

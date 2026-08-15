package com.alkacode.rankup.hook;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ponte opcional (estatica) com o ItemsAdder - usada so pra icone cosmetico de rank
 * (ver IconConfig#itemsAdderId, IconFactory). Mesmo padrao ja usado em
 * plugins/AlkaMines/hook/ItemsAdderHook.java - dependencia real via jitpack
 * (com.github.LoneDev6:API-ItemsAdder), nao reflexao: a referencia a CustomStack so
 * existe dentro de corpo de metodo (nunca campo/instanceof/.class), entao a JVM so
 * resolve a classe se o codigo realmente executar - guardado por {@link #isEnabled()}
 * em todo call site, nunca roda sem o ItemsAdder presente.
 */
public final class ItemsAdderHook {

    private static volatile boolean enabled = false;

    private ItemsAdderHook() {
    }

    public static void tryHook(JavaPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            return;
        }
        enabled = true;
        plugin.getLogger().info("Hook ItemsAdder ativado (icones de rank).");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static ItemStack getCustomItem(String namespace) {
        if (!enabled || namespace == null || namespace.isBlank()) {
            return null;
        }
        CustomStack stack = CustomStack.getInstance(namespace);
        return stack != null ? stack.getItemStack() : null;
    }
}

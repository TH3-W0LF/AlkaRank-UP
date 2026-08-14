package com.alkacode.rankup.util;

import com.alkacode.rankup.model.IconConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;

public final class IconFactory {

    private IconFactory() {
    }

    public static ItemStack build(IconConfig icon) {
        return build(icon, null, List.of());
    }

    /**
     * @param displayNameOverride usado quando o nome do botao vem de outro campo do
     *                            config (ex: rank.displayName(), kit.name()) em vez do
     *                            proprio IconConfig - passe {@code null} para usar
     *                            {@link IconConfig#displayName()}.
     */
    public static ItemStack build(IconConfig icon, String displayNameOverride, List<Component> lore) {
        ItemStack item = new ItemStack(icon.material());
        ItemMeta meta = item.getItemMeta();

        String name = displayNameOverride != null ? displayNameOverride : icon.displayName();
        if (name != null) {
            meta.displayName(TextUtil.parse(name));
        }
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        applyCustomModelData(meta, icon.customModelData());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * {@code ItemMeta#setCustomModelData(Integer)} esta depreciado desde que o
     * Minecraft passou a suportar multiplos floats/flags/strings de CustomModelData;
     * aplicamos via {@link CustomModelDataComponent} para nao usar API depreciada.
     */
    public static void applyCustomModelData(ItemMeta meta, int customModelData) {
        if (customModelData <= 0) {
            return;
        }
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFloats(List.of((float) customModelData));
        meta.setCustomModelDataComponent(component);
    }
}

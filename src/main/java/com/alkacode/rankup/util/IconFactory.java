package com.alkacode.rankup.util;

import com.alkacode.core.util.ItemBuilder;
import com.alkacode.rankup.hook.ItemsAdderHook;
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
        ItemStack item = buildBase(icon);
        ItemMeta meta = item.getItemMeta();

        String name = displayNameOverride != null ? displayNameOverride : icon.displayName();
        if (name != null) {
            meta.displayName(TextUtil.parse(name));
        }
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        if (!icon.hasItemsAdderId()) {
            // custom_model_data so faz sentido pra item vanilla - um item do ItemsAdder ja
            // vem com o proprio model data, sobrescrever quebraria a textura/modelo custom dele.
            applyCustomModelData(meta, icon.customModelData());
        }

        item.setItemMeta(meta);
        return item;
    }

    /** Resolve o ItemStack BASE (sem nome/lore proprios ainda) - prioridade: item/bloco
     * custom do ItemsAdder (se configurado e o plugin estiver presente) > cabeca com
     * textura Base64 > Material vanilla configurado. Nunca lanca - qualquer fonte
     * ausente/indisponivel cai pra proxima da lista. */
    private static ItemStack buildBase(IconConfig icon) {
        if (icon.hasItemsAdderId() && ItemsAdderHook.isEnabled()) {
            ItemStack custom = ItemsAdderHook.getCustomItem(icon.itemsAdderId());
            if (custom != null) {
                return custom;
            }
        }
        if (icon.hasTexture()) {
            return ItemBuilder.skullFromTexture(icon.texture());
        }
        return new ItemStack(icon.material());
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

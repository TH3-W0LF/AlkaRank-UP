package com.alkacode.rankup.model;

import org.bukkit.Material;

/**
 * {@code customModelData <= 0} significa "nao aplicar" - segue a convencao do
 * config.yml, onde 0 e o valor padrao/ausente. {@code displayName} e nulo quando o
 * icone nao carrega nome proprio (ex: icone de rank, cujo nome vem de outro campo).
 *
 * {@code texture} (Base64) e {@code itemsAdderId} sao opcionais - quando presentes tem
 * prioridade sobre {@code material} (itemsAdderId > texture > material), ver
 * IconFactory#buildBase. Nenhum dos dois e obrigatorio; sem eles o icone e so o
 * Material vanilla de sempre.
 */
public record IconConfig(Material material, int customModelData, String displayName, String texture, String itemsAdderId) {

    public IconConfig(Material material, int customModelData, String displayName) {
        this(material, customModelData, displayName, null, null);
    }

    public boolean hasTexture() {
        return texture != null && !texture.isBlank();
    }

    public boolean hasItemsAdderId() {
        return itemsAdderId != null && !itemsAdderId.isBlank();
    }
}

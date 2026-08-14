package com.alkacode.rankup.model;

import org.bukkit.Material;

/**
 * {@code customModelData <= 0} significa "nao aplicar" - segue a convencao do
 * config.yml, onde 0 e o valor padrao/ausente. {@code displayName} e nulo quando o
 * icone nao carrega nome proprio (ex: icone de rank, cujo nome vem de outro campo).
 */
public record IconConfig(Material material, int customModelData, String displayName) {
}

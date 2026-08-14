package com.alkacode.rankup.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/** Cabeca dropada ao matar um mob configurado (heads.types.*) - ver ConfigManager#headTypes / HeadDropListener. */
public record HeadType(String id, EntityType entityType, String displayName, Material material, String texture, double chance) {

    /** true = usa PLAYER_HEAD com textura Base64 custom; false = usa o material vanilla configurado (ex: ZOMBIE_HEAD). */
    public boolean hasTexture() {
        return texture != null && !texture.isBlank();
    }
}

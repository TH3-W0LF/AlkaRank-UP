package com.alkacode.rankup.database;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.rankup.model.KitType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KitCooldownRepository extends AbstractRepository {

    private final Logger logger;

    public KitCooldownRepository(DatabaseProvider db, Logger logger) {
        super(db);
        this.logger = logger;
        createTable();
    }

    private void createTable() {
        try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS alka_kits_cooldowns (
                        uuid VARCHAR(36) NOT NULL,
                        rank_id VARCHAR(64) NOT NULL,
                        kit_type VARCHAR(32) NOT NULL,
                        last_claimed BIGINT NOT NULL,
                        PRIMARY KEY (uuid, rank_id, kit_type)
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabela alka_kits_cooldowns", e);
        }
    }

    /** Carrega todos os cooldowns de um jogador numa unica query. Chave do mapa: {@code rankId + ":" + kitType}. */
    public Map<String, Long> loadCooldowns(UUID uuid) {
        Map<String, Long> cooldowns = new HashMap<>();
        String sql = "SELECT rank_id, kit_type, last_claimed FROM alka_kits_cooldowns WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cooldowns.put(rs.getString("rank_id") + ":" + rs.getString("kit_type"), rs.getLong("last_claimed"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar cooldowns de kit de " + uuid, e);
        }
        return cooldowns;
    }

    public void saveCooldown(UUID uuid, String rankId, KitType kitType, long timestamp) {
        String sql = upsert("alka_kits_cooldowns",
                new String[]{"uuid", "rank_id", "kit_type", "last_claimed"},
                new String[]{"uuid", "rank_id", "kit_type"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, rankId);
                ps.setString(3, kitType.name());
                ps.setLong(4, timestamp);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar cooldown de kit de " + uuid, e);
        }
    }
}

package com.alkacode.rankup.database;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Banco (armazenamento) de cabecas depositadas por jogador - ver HeadsManager. */
public final class HeadsRepository extends AbstractRepository {

    private final Logger logger;

    public HeadsRepository(DatabaseProvider db, Logger logger) {
        super(db);
        this.logger = logger;
        createTable();
    }

    private void createTable() {
        try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rankup_heads (
                        uuid VARCHAR(36) NOT NULL,
                        head_id VARCHAR(32) NOT NULL,
                        amount INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (uuid, head_id)
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabela rankup_heads", e);
        }
    }

    /** Chave do mapa retornado: head_id. */
    public Map<String, Integer> loadBanked(UUID uuid) {
        Map<String, Integer> banked = new HashMap<>();
        String sql = "SELECT head_id, amount FROM rankup_heads WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    banked.put(rs.getString("head_id"), rs.getInt("amount"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar heads de " + uuid, e);
        }
        return banked;
    }

    public void saveAmount(UUID uuid, String headId, int amount) {
        String sql = upsert("rankup_heads",
                new String[]{"uuid", "head_id", "amount"},
                new String[]{"uuid", "head_id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, headId);
                ps.setInt(3, amount);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar heads de " + uuid, e);
        }
    }
}

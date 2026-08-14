package com.alkacode.rankup.database;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PrestigeRewardsRepository extends AbstractRepository {

    private final Logger logger;

    public PrestigeRewardsRepository(DatabaseProvider db, Logger logger) {
        super(db);
        this.logger = logger;
        createTable();
    }

    private void createTable() {
        try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rankup_prestige_rewards_claimed (
                        uuid VARCHAR(36) NOT NULL,
                        prestige_level INTEGER NOT NULL,
                        PRIMARY KEY (uuid, prestige_level)
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabela rankup_prestige_rewards_claimed", e);
        }
    }

    /** Todos os niveis de prestigio cuja recompensa UNICA ja foi resgatada por este jogador. */
    public Set<Integer> loadClaimed(UUID uuid) {
        Set<Integer> claimed = new HashSet<>();
        String sql = "SELECT prestige_level FROM rankup_prestige_rewards_claimed WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    claimed.add(rs.getInt("prestige_level"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar recompensas de prestigio resgatadas de " + uuid, e);
        }
        return claimed;
    }

    public void markClaimed(UUID uuid, int prestigeLevel) {
        String sql = db.isSQLite()
                ? "INSERT OR IGNORE INTO rankup_prestige_rewards_claimed (uuid, prestige_level) VALUES (?, ?)"
                : "INSERT IGNORE INTO rankup_prestige_rewards_claimed (uuid, prestige_level) VALUES (?, ?)";
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setInt(2, prestigeLevel);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao marcar recompensa de prestigio resgatada de " + uuid, e);
        }
    }
}

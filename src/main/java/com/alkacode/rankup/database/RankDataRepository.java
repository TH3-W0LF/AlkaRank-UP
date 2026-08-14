package com.alkacode.rankup.database;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.rankup.model.PlayerRankData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unico ponto de acesso ao banco de rank/prestigio do AlkaRankUp, sobre o
 * {@link DatabaseProvider} do AlkaCore (R2 - zero JDBC proprio). Chamadas bloqueantes
 * de proposito (mesmo padrao de TimeRepository) - quem chama roda fora da main thread (R7).
 */
public final class RankDataRepository extends AbstractRepository {

    private final Logger logger;

    public RankDataRepository(DatabaseProvider db, Logger logger) {
        super(db);
        this.logger = logger;
        createTable();
    }

    private void createTable() {
        try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rankup_players (
                        uuid VARCHAR(36) PRIMARY KEY,
                        rank_index INTEGER NOT NULL DEFAULT 0,
                        prestige_level INTEGER NOT NULL DEFAULT 0
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabela rankup_players", e);
        }
    }

    /** SELECT + INSERT (quando ausente) na mesma chamada, pra linha do jogador sempre existir apos o primeiro load. */
    public PlayerRankData loadOrInit(UUID uuid) {
        String sql = "SELECT rank_index, prestige_level FROM rankup_players WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerRankData(uuid, rs.getInt("rank_index"), rs.getInt("prestige_level"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar dados de " + uuid, e);
            return new PlayerRankData(uuid, 0, 0);
        }

        PlayerRankData fresh = new PlayerRankData(uuid, 0, 0);
        save(fresh);
        return fresh;
    }

    public void save(PlayerRankData data) {
        String sql = upsert("rankup_players",
                new String[]{"uuid", "rank_index", "prestige_level"},
                new String[]{"uuid"});
        try {
            execute(sql, ps -> {
                ps.setString(1, data.uuid().toString());
                ps.setInt(2, data.rankIndex());
                ps.setInt(3, data.prestigeLevel());
            });
            data.clearDirty();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar dados de " + data.uuid(), e);
        }
    }

    /** Ranking pra /rankup top - maior prestigio primeiro, rank como desempate. */
    public List<PlayerRankData> topPlayers(int limit) {
        List<PlayerRankData> result = new ArrayList<>();
        String sql = "SELECT uuid, rank_index, prestige_level FROM rankup_players ORDER BY prestige_level DESC, rank_index DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new PlayerRankData(UUID.fromString(rs.getString("uuid")),
                            rs.getInt("rank_index"), rs.getInt("prestige_level")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar top jogadores", e);
        }
        return result;
    }
}

package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.Game;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.gamersafer.minecraft.abbacaving.game.PlayerWinEntry;
import com.gamersafer.minecraft.abbacaving.tools.CosmeticRegistry;
import com.gamersafer.minecraft.abbacaving.tools.impl.SlottedHotbarTool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

public class SQLDataSource implements DataSource {

    private final AbbaCavingPlugin plugin;
    private HikariDataSource dataSource;

    public SQLDataSource(final AbbaCavingPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() throws IllegalStateException, IllegalArgumentException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (final ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }

        final String host = this.plugin.getConfig().getString("mysql.host");
        final String name = this.plugin.getConfig().getString("mysql.database-name");

        final HikariConfig config = new HikariConfig();
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(4);
        config.setJdbcUrl("jdbc:mariadb://" + host + "/" + name);
        config.setUsername(this.plugin.getConfig().getString("mysql.username"));
        config.setPassword(this.plugin.getConfig().getString("mysql.password"));

        this.dataSource = new HikariDataSource(config);

        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, this::createTable);
    }

    private void createTable() {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final Statement statement = conn.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS abba_caving_stats (uuid VARCHAR(50) PRIMARY KEY, wins INT, highest_score INT, ores_mined INT);");
                statement.execute("CREATE TABLE IF NOT EXISTS abba_hotbar_layout (uuid VARCHAR(50) NOT NULL, slot INT NOT NULL, material VARCHAR(50), CONSTRAINT layout_pk PRIMARY KEY(uuid, slot));");
                statement.execute("CREATE TABLE IF NOT EXISTS abba_cosmetics (uuid VARCHAR(50) NOT NULL, cosmetic VARCHAR(50));");
                statement.execute("CREATE TABLE IF NOT EXISTS abba_respawns (uuid VARCHAR(50) PRIMARY KEY, respawns INT);");
                statement.execute("CREATE TABLE IF NOT EXISTS abba_round_leaderboard (round_id VARCHAR(50) NOT NULL, place INT NOT NULL, player VARCHAR(50) NOT NULL, score INT NOT NULL, PRIMARY KEY(round_id, place));");
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void loadPlayerStats(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            // Load game stats
            try (final PreparedStatement stmt = conn.prepareStatement("SELECT wins, highest_score, ores_mined FROM abba_caving_stats WHERE uuid = ?;")) {
                stmt.setString(1, gp.playerUUID().toString());

                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        gp.wins(rs.getInt("wins"));
                        gp.highestScore(rs.getInt("highest_score"));
                        gp.totalOresMined(rs.getInt("ores_mined"));
                    }
                }
            }

            // Load hotbar layout
            try (final PreparedStatement hotbarStatement = conn.prepareStatement("SELECT * FROM abba_hotbar_layout WHERE uuid = ?;")) {
                hotbarStatement.setString(1, gp.playerUUID().toString());

                try (final ResultSet rs = hotbarStatement.executeQuery()) {
                    while (rs.next()) {
                        gp.hotbarLayout(SlottedHotbarTool.stored(rs.getString("material")), rs.getInt("slot"));
                    }
                }
            }

            // Load selected cosmetics
            try (final PreparedStatement cosmeticsStatement = conn.prepareStatement("SELECT * FROM abba_cosmetics WHERE uuid = ?;")) {
                cosmeticsStatement.setString(1, gp.playerUUID().toString());

                try (final ResultSet rs = cosmeticsStatement.executeQuery()) {
                    while (rs.next()) {
                        final CosmeticRegistry.Cosmetic cosmetic = this.plugin.cosmeticRegistry().get(rs.getString("cosmetic"));
                        gp.addSelectedCosmetic(cosmetic.toolType(), cosmetic);
                    }
                }
            }

            this.updatePlayerRespawns(gp);
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void savePlayerStats(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO abba_caving_stats (uuid, wins, highest_score, ores_mined) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE wins = ?, highest_score = ?, ores_mined = ?;")) {
                stmt.setString(1, gp.playerUUID().toString());
                stmt.setInt(2, gp.wins());
                stmt.setInt(3, gp.highestScore());
                stmt.setInt(4, gp.totalOresMined());
                stmt.setInt(5, gp.wins());
                stmt.setInt(6, gp.highestScore());
                stmt.setInt(7, gp.totalOresMined());
                stmt.executeUpdate();
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }

        this.savePlayerCosmetics(gp);
        this.savePlayerHotbar(gp);
    }

    @Override
    public void savePlayerHotbar(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            for (final Map.Entry<SlottedHotbarTool, Integer> entry : gp.hotbarLayout().entrySet()) {
                try (final PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO abba_hotbar_layout (uuid, slot, material) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE slot = ?, material = ?;")) {
                    stmt.setString(1, gp.playerUUID().toString());
                    stmt.setInt(2, entry.getValue());
                    stmt.setString(3, entry.getKey().identifier());
                    stmt.setInt(4, entry.getValue());
                    stmt.setString(5, entry.getKey().identifier());

                    stmt.executeUpdate();
                } catch (final SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void savePlayerCosmetics(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            for (final CosmeticRegistry.Cosmetic cosmetic : gp.selectedCosmetics()) {
                try (final PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO abba_cosmetics (uuid, cosmetic) VALUES (?, ?);")) {
                    stmt.setString(1, gp.playerUUID().toString());
                    stmt.setString(2, cosmetic.identifier());

                    stmt.executeUpdate();
                } catch (final SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    //CREATE TABLE IF NOT EXISTS abba_round_leaderboard (round_id VARCHAR(50) NOT NULL, place INT NOT NULL, player VARCHAR(50) NOT NULL, score INT NOT NULL, PRIMARY KEY(round_id, place));
    @Override
    public void saveFinishedGame(final Game game) {
        try (final Connection conn = this.dataSource.getConnection()) {
            int entryNum = 0;
            for (final Map.Entry<GamePlayer, Integer> entry : game.leaderboard().entrySet()) {
                try (final PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO abba_round_leaderboard (round_id, place, player, score) VALUES (?, ?, ?, ?)")) {
                    stmt.setString(1, game.gameId());
                    stmt.setInt(2, entryNum);
                    stmt.setString(3, entry.getKey().playerUUID().toString());
                    stmt.setInt(4, entry.getValue());

                    stmt.executeUpdate();
                } catch (final SQLException ex) {
                    ex.printStackTrace();
                }
                entryNum++;
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public PlayerWinEntry winEntry(final String gameId, final int place) {
        try (final Connection conn = this.dataSource.getConnection()) {
            // Load game stats
            try (final PreparedStatement stmt = conn.prepareStatement("SELECT player, score FROM abba_round_leaderboard WHERE round_id = ? AND place = ?;")) {
                stmt.setString(1, gameId);
                stmt.setInt(2, place);

                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        final int points = rs.getInt("score");
                        final UUID player = UUID.fromString(rs.getString("player"));
                        return new PlayerWinEntry(player, points);
                    }

                    return null;
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public PlayerWinEntry globalWinEntry(int place) {
        try (final Connection conn = this.dataSource.getConnection()) {
            // Load game stats
            try (final PreparedStatement stmt = conn.prepareStatement("SELECT player, score\n" +
                    "FROM abba_round_leaderboard GROUP BY player ORDER BY score DESC LIMIT 1 OFFSET ?;")) {
                stmt.setInt(1, place - 1);

                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        final int points = rs.getInt("score");
                        final UUID player = UUID.fromString(rs.getString("player"));
                        return new PlayerWinEntry(player, points);
                    }

                    return null;
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public void updatePlayerRespawns(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final PreparedStatement stmt = conn.prepareStatement("SELECT respawns FROM abba_respawns WHERE uuid = ?;")) {
                stmt.setString(1, gp.playerUUID().toString());

                int respawns = 0;
                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        respawns = rs.getInt("respawns");
                    }
                }
                gp.respawns(respawns);
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void savePlayerRespawns(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO abba_respawns (uuid, respawns) VALUES (?, ?) ON DUPLICATE KEY UPDATE respawns = ?;")) {
                stmt.setString(1, gp.playerUUID().toString());
                stmt.setInt(2, gp.respawns());
                stmt.setInt(3, gp.respawns());
                stmt.executeUpdate();
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

}

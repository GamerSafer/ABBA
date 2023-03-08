package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.AbbaCavingPlugin;
import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class SQLDataSource implements PlayerDataSource {

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
                stmt.setString(1, gp.player().getUniqueId().toString());

                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        gp.wins(rs.getInt("wins"));
                        gp.highestScore(rs.getInt("highest_score"));
                        gp.totalOresMined(rs.getInt("ores_mined"));
                        this.plugin.getLogger().info("Loaded " + gp.player().getName() + "'s stats");
                    } else {
                        this.plugin.getLogger().info("No stats found for player " + gp.player().getName());
                    }
                }
            }

            // Load hotbar layout
            try (final PreparedStatement hotbarStatement = conn.prepareStatement("SELECT * FROM abba_hotbar_layout WHERE uuid = ?;")) {
                hotbarStatement.setString(1, gp.player().getUniqueId().toString());

                try (final ResultSet rs = hotbarStatement.executeQuery()) {
                    while (rs.next()) {
                        gp.hotbarLayout(rs.getInt("slot"), rs.getString("material"));
                    }

                    if (gp.hasCustomHotbarLayout()) {
                        this.plugin.getLogger().info("Loaded " + gp.player().getName() + "'s hotbar layout");
                    }
                }
            }

            // Load selected cosmetics
            try (final PreparedStatement cosmeticsStatement = conn.prepareStatement("SELECT * FROM abba_cosmetics WHERE uuid = ?;")) {
                cosmeticsStatement.setString(1, gp.player().getUniqueId().toString());

                try (final ResultSet rs = cosmeticsStatement.executeQuery()) {
                    while (rs.next()) {
                        gp.addSelectedCosmetic(rs.getString("cosmetic"));
                    }
                }
            }
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
            for (final Map.Entry<Integer, String> entry : gp.hotbarLayout().entrySet()) {
                try (final PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO abba_hotbar_layout (uuid, slot, material) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE slot = ?, material = ?;")) {
                    stmt.setString(1, gp.player().getUniqueId().toString());
                    stmt.setInt(2, entry.getKey());
                    stmt.setString(3, entry.getValue());
                    stmt.setInt(4, entry.getKey());
                    stmt.setString(5, entry.getValue());

                    stmt.executeUpdate();
                } catch (final SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }

        this.plugin.getLogger().info("Saved " + gp.player().getName() + "'s hotbar");
    }

    @Override
    public void updatePlayerRespawns(GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final PreparedStatement stmt = conn.prepareStatement("SELECT respawns FROM abba_respawns WHERE uuid = ?;")) {
                stmt.setString(1, gp.player().getUniqueId().toString());

                int respawns = 0;
                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        respawns = rs.getInt("respawns");
                        this.plugin.getLogger().info("Loaded " + gp.player().getName() + "'s respawns");
                    } else {
                        this.plugin.getLogger().info("No respawns found for player " + gp.player().getName());
                    }
                }
                gp.setRespawns(respawns);
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void savePlayerRespawns(GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO abba_respawns (uuid, points) VALUES (?, ?) ON DUPLICATE KEY UPDATE points = ?;")) {
                stmt.setString(1, gp.playerUUID().toString());
                stmt.setInt(2, gp.getRespawns());
                stmt.executeUpdate();
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void savePlayerCosmetics(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            for (final String cosmetic : gp.selectedCosmetics()) {
                try (final PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO abba_cosmetics (uuid, cosmetic) VALUES (?, ?);")) {
                    stmt.setString(1, gp.player().getUniqueId().toString());
                    stmt.setString(2, cosmetic);

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
    public void updatePlayerRespawns(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final PreparedStatement stmt = conn.prepareStatement("SELECT respawns FROM abba_respawns WHERE uuid = ?;")) {
                stmt.setString(1, gp.player().getUniqueId().toString());

                int respawns = 0;
                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        respawns = rs.getInt("respawns");
                        this.plugin.getLogger().info("Loaded " + gp.player().getName() + "'s respawns");
                    } else {
                        this.plugin.getLogger().info("No respawns found for player " + gp.player().getName());
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
                    "INSERT INTO abba_respawns (uuid, points) VALUES (?, ?) ON DUPLICATE KEY UPDATE points = ?;")) {
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

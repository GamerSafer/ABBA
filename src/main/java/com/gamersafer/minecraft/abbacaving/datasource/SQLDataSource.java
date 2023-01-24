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
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void loadPlayerStats(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
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

            try (final PreparedStatement hotbarStatement = conn.prepareStatement("SELECT * FROM abba_hotbar_layout WHERE uuid =  ?;")) {
                hotbarStatement.setString(1, gp.player().getUniqueId().toString());

                try (final ResultSet rs = hotbarStatement.executeQuery()) {
                    if (rs.next()) {
                        gp.hotbarLayout(rs.getInt("slot"), rs.getString("material"));

                        this.plugin.getLogger().info("Loaded " + gp.player().getName() + "'s hotbar layout");
                    } else {
                        this.plugin.getLogger().info("No hotbar layout found for player " + gp.player().getName());
                    }
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void savePlayerStats(final GamePlayer gp) {
        try (final Connection conn = this.dataSource.getConnection()) {
            try (final PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO abba_caving_stats (uuid, wins, highest_score, ores_mined) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE wins = ?, highest_score = ?, ores_mined = ?;")) {
                stmt.setString(1, gp.player().getUniqueId().toString());
                stmt.setInt(2, gp.wins());
                stmt.setInt(3, gp.highestScore());
                stmt.setInt(4, gp.totalOresMined());
                stmt.setInt(5, gp.wins());
                stmt.setInt(6, gp.highestScore());
                stmt.setInt(7, gp.totalOresMined());
                stmt.executeUpdate();
                this.plugin.getLogger().info("Saved " + gp.player().getName() + "'s stats");
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

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

}

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
            }
        } catch (final SQLException exception) {
            exception.printStackTrace();
        }

        try(final Connection conn = this.dataSource.getConnection()) {
            try (final Statement statement = conn.createStatement()) {
                statement.execute("ALTER TABLE abba_caving_stats ADD COLUMN total_games INT AFTER ores_mined;");
            }
        } catch (final SQLException ignored) {
            // An exception is thrown if the column already exists, we don't want to log that
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
                        gp.totalGames(rs.getInt("total_games"));
                        this.plugin.getLogger().info("Loaded " + gp.player().getName() + "'s stats");
                    } else {
                        this.plugin.getLogger().info("No stats found for player " + gp.player().getName());
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
                    "INSERT INTO abba_caving_stats (uuid, wins, highest_score, ores_mined, total_games) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE wins = ?, highest_score = ?, ores_mined = ?, total_games = ?;")) {
                stmt.setString(1, gp.player().getUniqueId().toString());

                stmt.setInt(2, gp.wins());
                stmt.setInt(3, gp.highestScore());
                stmt.setInt(4, gp.totalOresMined());
                stmt.setInt(5, gp.totalGames());

                stmt.setInt(6, gp.wins());
                stmt.setInt(7, gp.highestScore());
                stmt.setInt(8, gp.totalOresMined());
                stmt.setInt(9, gp.totalGames());

                stmt.executeUpdate();
                this.plugin.getLogger().info("Saved " + gp.player().getName() + "'s stats");
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

}

package com.rootssky.battlepass.managers;

import com.rootssky.battlepass.BattlePassPlugin;
import com.rootssky.battlepass.models.PlayerProfile;
import com.rootssky.battlepass.utils.Utils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {

    private final BattlePassPlugin plugin;
    private final File dataFolder;
    private volatile Connection connection;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Object connectionLock = new Object();

    private static final String SQL_UPSERT = """
            INSERT INTO players (uuid, level, xp, premium, last_login, completed_missions, claimed_rewards)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                level = excluded.level,
                xp = excluded.xp,
                premium = excluded.premium,
                last_login = excluded.last_login,
                completed_missions = excluded.completed_missions,
                claimed_rewards = excluded.claimed_rewards
            """;

    public DatabaseManager(BattlePassPlugin plugin, File dataFolder) {
        this.plugin = plugin;
        this.dataFolder = dataFolder;
    }

    public void init() {
        CompletableFuture.runAsync(() -> {
            try {
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }

                String url = "jdbc:sqlite:" + new File(dataFolder, "battlepass.db").getAbsolutePath();
                Connection conn = DriverManager.getConnection(url);

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL");
                    stmt.execute("PRAGMA synchronous=NORMAL");
                    stmt.execute("PRAGMA busy_timeout=5000");

                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS players (
                            uuid TEXT PRIMARY KEY,
                            level INTEGER NOT NULL DEFAULT 1,
                            xp INTEGER NOT NULL DEFAULT 0,
                            premium INTEGER NOT NULL DEFAULT 0,
                            last_login TEXT NOT NULL DEFAULT ''
                        )
                        """);

                    try {
                        stmt.execute("ALTER TABLE players ADD COLUMN completed_missions TEXT NOT NULL DEFAULT ''");
                    } catch (SQLException ignored) {
                    }

                    try {
                        stmt.execute("ALTER TABLE players ADD COLUMN claimed_rewards TEXT NOT NULL DEFAULT ''");
                    } catch (SQLException ignored) {
                    }
                }

                synchronized (connectionLock) {
                    connection = conn;
                }

                Utils.log("<green>Banco de dados inicializado com sucesso!");
            } catch (SQLException e) {
                throw new RuntimeException("Falha ao inicializar o banco de dados", e);
            }
        }, executor).join();
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    String url = "jdbc:sqlite:" + new File(dataFolder, "battlepass.db").getAbsolutePath();
                    connection = DriverManager.getConnection(url);
                }
            }
        }
        return connection;
    }

    public CompletableFuture<PlayerProfile> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM players WHERE uuid = ?";
                try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            PlayerProfile profile = new PlayerProfile(uuid);
                            profile.setLevel(rs.getInt("level"));
                            profile.setXp(rs.getInt("xp"));
                            profile.setPremium(rs.getInt("premium") == 1);

                            String missionsStr = rs.getString("completed_missions");
                            if (missionsStr != null && !missionsStr.isEmpty()) {
                                for (String m : missionsStr.split(",")) {
                                    if (!m.isBlank()) {
                                        profile.getCompletedMissions().add(m.trim());
                                    }
                                }
                            }

                            String rewardsStr = rs.getString("claimed_rewards");
                            if (rewardsStr != null && !rewardsStr.isEmpty()) {
                                for (String r : rewardsStr.split(",")) {
                                    if (!r.isBlank()) {
                                        profile.getClaimedRewards().add(r.trim());
                                    }
                                }
                            }

                            String formula = plugin.getConfigManager().getXpFormula();
                            profile.setXpFormula(formula);

                            return profile;
                        }
                    }
                }

                PlayerProfile newProfile = new PlayerProfile(uuid);
                newProfile.setXpFormula(plugin.getConfigManager().getXpFormula());
                savePlayerSync(newProfile);
                return newProfile;
            } catch (SQLException e) {
                throw new RuntimeException("Falha ao carregar jogador: " + uuid, e);
            }
        }, executor);
    }

    public void savePlayerSync(PlayerProfile profile) {
        synchronized (connectionLock) {
            try {
                try (PreparedStatement ps = getConnection().prepareStatement(SQL_UPSERT)) {
                    ps.setString(1, profile.getUuid().toString());
                    ps.setInt(2, profile.getLevel());
                    ps.setInt(3, profile.getXp());
                    ps.setInt(4, profile.isPremium() ? 1 : 0);
                    ps.setString(5, java.time.Instant.now().toString());
                    ps.setString(6, String.join(",", profile.getCompletedMissions()));
                    ps.setString(7, String.join(",", profile.getClaimedRewards()));
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Falha ao salvar jogador (sync): " + profile.getUuid(), e);
            }
        }
    }

    public CompletableFuture<Void> savePlayer(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> savePlayerSync(profile), executor);
    }

    public CompletableFuture<Void> flushAll(Collection<PlayerProfile> profiles) {
        return CompletableFuture.runAsync(() -> {
            synchronized (connectionLock) {
                try {
                    getConnection().setAutoCommit(false);

                    try (PreparedStatement ps = getConnection().prepareStatement(SQL_UPSERT)) {
                        for (PlayerProfile profile : profiles) {
                            try {
                                ps.setString(1, profile.getUuid().toString());
                                ps.setInt(2, profile.getLevel());
                                ps.setInt(3, profile.getXp());
                                ps.setInt(4, profile.isPremium() ? 1 : 0);
                                ps.setString(5, java.time.Instant.now().toString());
                                ps.setString(6, String.join(",", profile.getCompletedMissions()));
                                ps.setString(7, String.join(",", profile.getClaimedRewards()));
                                ps.addBatch();
                            } catch (SQLException e) {
                                Utils.log("<red>Erro ao adicionar profile ao batch: " + profile.getUuid() + " - " + e.getMessage());
                            }
                        }
                        ps.executeBatch();
                    }

                    getConnection().setAutoCommit(true);
                    Utils.log("<green>Flush do banco: " + profiles.size() + " jogadores salvos.");
                } catch (SQLException e) {
                    try {
                        getConnection().rollback();
                        getConnection().setAutoCommit(true);
                    } catch (SQLException ignored) {
                    }

                    Utils.log("<red>Flush em batch falhou, salvando individualmente...");
                    for (PlayerProfile profile : profiles) {
                        try {
                            savePlayerSync(profile);
                        } catch (Exception ex) {
                            Utils.log("<red>Falha ao salvar " + profile.getUuid() + ": " + ex.getMessage());
                        }
                    }
                    Utils.log("<yellow>Flush individual concluído: " + profiles.size() + " jogadores.");
                }
            }
        }, executor);
    }

    public void close() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        synchronized (connectionLock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Falha ao fechar o banco de dados", e);
            }
        }
    }
}

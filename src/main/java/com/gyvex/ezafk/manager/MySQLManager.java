package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQLManager provides robust, asynchronous management of all MySQL operations for EzAfk.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Connection lifecycle management</li>
 *   <li>Thread-safe, asynchronous query execution to keep the Bukkit main thread responsive</li>
 *   <li>Resource cleanup and error handling</li>
 *   <li>CRUD operations for AFK player data</li>
 * </ul>
 * All mutating queries are dispatched via a dedicated executor to ensure optimal server performance.
 * </p>
 */
public class MySQLManager {
    private static Connection connection;
    private static String host;
    private static String database;
    private static String username;
    private static String password;
    private static int port;
    private static boolean enabled;
    private static ExecutorService executor;
    private static final Logger LOGGER = Logger.getLogger(MySQLManager.class.getName());

    /**
     * Initializes the MySQL connection and executor service using configuration values.
     * <p>
     * Validates all required fields and logs clear errors if any are missing or invalid.
     * If MySQL is disabled, ensures all resources are properly released.
     * </p>
     */
    public static void setup() {
        FileConfiguration mysqlConfig = Registry.get().getConfigManager().getMysqlConfig();

        enabled = mysqlConfig.getBoolean("enabled");
        if (!enabled) {
            closeConnection();
            shutdownExecutor();
            LOGGER.info("MySQL is disabled in configuration.");
            return;
        }

        host = mysqlConfig.getString("host");
        port = mysqlConfig.getInt("port");
        database = mysqlConfig.getString("database");
        username = mysqlConfig.getString("username");
        password = mysqlConfig.getString("password");

        if (host == null || host.isEmpty() || database == null || database.isEmpty()
                || username == null || username.isEmpty() || password == null) {
            LOGGER.severe("MySQL configuration is invalid. Please check host, database, username, and password fields.");
            enabled = false;
            return;
        }

        try {
            ensureExecutor();
            openConnection();
            LOGGER.info("MySQL connection established successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not connect to MySQL server!", e);
        }
    }

    /**
     * Opens a new MySQL connection if none exists or the current one is invalid.
     * Uses connection validation to avoid unnecessary reconnects.
     *
     * @throws SQLException if a connection cannot be established
     */
    private static void openConnection() throws SQLException {
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(2)) {
                    return;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "MySQL connection validation failed, attempting to reconnect.", e);
            }
        }

        synchronized (MySQLManager.class) {
            if (connection == null || connection.isClosed() || !isConnectionValid(connection)) {
                connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true", username, password);
            }
        }
    }

    /**
     * Checks if the given MySQL connection is valid and open.
     *
     * @param conn the connection to check
     * @return true if valid, false otherwise
     */
    private static boolean isConnectionValid(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "MySQL connection validation threw an exception.", e);
            return false;
        }
    }

    /**
     * Closes the current MySQL connection, logging any errors encountered.
     */
    private static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Could not close MySQL connection!", e);
            }
        }
        connection = null;
    }

    /**
     * Indicates whether MySQL is enabled in the configuration and available for use.
     *
     * @return true if MySQL is enabled, false otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if the MySQL connection is currently valid and available.
     * Attempts to reconnect if necessary.
     *
     * @return true if connected, false otherwise
     */
    public static boolean isConnected() {
        if (!enabled) {
            closeConnection();
            return false;
        }

        try {
            if (connection == null || connection.isClosed() || !isConnectionValid(connection)) {
                openConnection();
            }
            return isConnectionValid(connection);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "MySQL connection is not available!", e);
            return false;
        }
    }

    /**
     * Runs a database operation asynchronously on the dedicated executor.
     * All database mutations are queued to avoid blocking the Bukkit main thread.
     *
     * @param task the operation to run
     */
    private static void runAsync(Runnable task) {
        if (!enabled) {
            return;
        }

        ExecutorService service;

        synchronized (MySQLManager.class) {
            ensureExecutor();
            service = executor;
        }

        if (service == null) {
            LOGGER.severe("MySQL executor is not available to schedule a task.");
            return;
        }

        service.execute(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                LOGGER.log(Level.SEVERE, "Unexpected error during asynchronous MySQL task", exception);
            }
        });
    }

    /**
     * Ensures the async executor is available for MySQL operations.
     * Creates a new single-threaded executor if none exists or if the previous one was shut down.
     */
    private static void ensureExecutor() {
        synchronized (MySQLManager.class) {
            if (executor != null && !executor.isShutdown() && !executor.isTerminated()) {
                return;
            }

            ThreadFactory threadFactory = new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "EzAfk-MySQL-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            };

            executor = Executors.newSingleThreadExecutor(threadFactory);
        }
    }

    /**
     * Shuts down the async executor for MySQL operations, releasing all resources.
     */
    private static void shutdownExecutor() {
        synchronized (MySQLManager.class) {
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }
    }

    /**
     * Adds or updates an AFK player entry in the database, retrying once on failure.
     *
     * @param playerId   the player's UUID
     * @param lastActive the last active timestamp
     */
    private static void executeAddOrUpdateAfkPlayer(UUID playerId, long lastActive) {
        int attempts = 0;
        while (attempts < 2) {
            if (!isConnected()) {
                attempts++;
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                continue;
            }
            try {
                String selectQuery = "SELECT COUNT(*) FROM afk_players WHERE player_id = ?";
                try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
                    selectStatement.setString(1, playerId.toString());
                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        if (resultSet.next() && resultSet.getInt(1) > 0) {
                            String updateQuery = "UPDATE afk_players SET last_active = ? WHERE player_id = ?";
                            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                                updateStatement.setLong(1, lastActive);
                                updateStatement.setString(2, playerId.toString());
                                updateStatement.executeUpdate();
                            }
                        } else {
                            String insertQuery = "INSERT INTO afk_players (player_id, last_active) VALUES (?, ?)";
                            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                                insertStatement.setString(1, playerId.toString());
                                insertStatement.setLong(2, lastActive);
                                insertStatement.executeUpdate();
                            }
                        }
                    }
                }
                break;
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "MySQL operation failed, retrying if possible", e);
                closeConnection();
                attempts++;
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Removes an AFK player entry from the database, retrying once on failure.
     *
     * @param playerId the player's UUID
     */
    private static void executeRemoveAfkPlayer(UUID playerId) {
        int attempts = 0;
        while (attempts < 2) {
            if (!isConnected()) {
                attempts++;
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                continue;
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM afk_players WHERE player_id = ?")) {
                statement.setString(1, playerId.toString());
                statement.executeUpdate();
                break;
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "MySQL remove operation failed, retrying if possible", e);
                closeConnection();
                attempts++;
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Updates the last active timestamp for an AFK player entry, retrying once on failure.
     *
     * @param playerId   the player's UUID
     * @param lastActive the last active timestamp
     */
    private static void executeUpdateLastActive(UUID playerId, long lastActive) {
        int attempts = 0;
        while (attempts < 2) {
            if (!isConnected()) {
                attempts++;
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                continue;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE afk_players SET last_active = ? WHERE player_id = ?")) {
                statement.setLong(1, lastActive);
                statement.setString(2, playerId.toString());
                statement.executeUpdate();
                break;
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "MySQL update operation failed, retrying if possible", e);
                closeConnection();
                attempts++;
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Adds or updates the AFK player entry asynchronously. Database writes are queued and executed away from
     * the Bukkit main thread for optimal performance.
     */
    public static void addAfkPlayerAsync(UUID playerId, long lastActive) {
        runAsync(() -> executeAddOrUpdateAfkPlayer(playerId, lastActive));
    }

    /**
     * Removes the AFK player entry asynchronously. The delete operation is queued on the database executor.
     */
    public static void removeAfkPlayerAsync(UUID playerId) {
        runAsync(() -> executeRemoveAfkPlayer(playerId));
    }

    /**
     * Updates the AFK player's last active timestamp asynchronously.
     */
    public static void updateLastActiveAsync(UUID playerId, long lastActive) {
        runAsync(() -> executeUpdateLastActive(playerId, lastActive));
    }

    /**
     * Retrieves the last active timestamp for a player from the database.
     *
     * @param playerId the player's UUID
     * @return the last active timestamp, or 0 if not found or on error
     */
    public static long getLastActive(UUID playerId) {
        long lastActive = 0;

        if (!enabled || !isConnected()) {
            return lastActive;
        }

        try {
            String query = "SELECT last_active FROM afk_players WHERE player_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, playerId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        lastActive = resultSet.getLong("last_active");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not retrieve last active time from MySQL!", e);
        }

        return lastActive;
    }

    /**
     * Checks if a player exists in the AFK database table.
     *
     * @param playerId the player's UUID
     * @return true if the player exists, false otherwise
     */
    public static boolean containsAfkPlayer(UUID playerId) {
        if (!enabled || !isConnected()) {
            return false;
        }

        try {
            String query = "SELECT 1 FROM afk_players WHERE player_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, playerId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not check if AFK player exists in MySQL!", e);
            return false;
        }
    }

    /**
     * Shuts down the MySQL manager, closing the connection and executor, and releasing all resources.
     */
    public static void shutdown() {
        closeConnection();
        shutdownExecutor();
    }
}

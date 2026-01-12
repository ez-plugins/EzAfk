package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class AfkTimeManager {
    private static final Map<UUID, Long> totalAfkSeconds = new ConcurrentHashMap<>();
    private static final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> pendingSaves = new ConcurrentHashMap<>();
    private static final int LEADERBOARD_CACHE_SIZE = 100;
    private static final NavigableSet<LeaderboardEntry> leaderboardCache = new TreeSet<>((first, second) -> {
        int comparison = Long.compare(second.totalSeconds, first.totalSeconds);
        if (comparison != 0) {
            return comparison;
        }
        return first.playerId.compareTo(second.playerId);
    });
    private static final Map<UUID, LeaderboardEntry> leaderboardEntries = new HashMap<>();
    private static File timesDirectory;
    private static BukkitTask flushTask;
    private static long flushIntervalTicks = 20L * 30L;
    private static final long DEFAULT_FLUSH_INTERVAL_SECONDS = 30L;
    private AfkTimeManager() {
    }

    public static void load(EzAfk plugin) {
        if (plugin == null) {
            return;
        }

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Unable to create EzAfk data folder for AFK time storage.");
            return;
        }

        stopFlushTask();

        totalAfkSeconds.clear();
        dirtyPlayers.clear();
        pendingSaves.clear();
        clearLeaderboardCache();

        timesDirectory = new File(dataFolder, "afk-times");
        if (!timesDirectory.exists() && !timesDirectory.mkdirs()) {
            plugin.getLogger().warning("Unable to create afk-times directory for AFK time storage.");
        }

        migrateLegacyData(plugin, new File(dataFolder, "afk-times.yml"));

        loadPlayerFiles(plugin);
        rebuildLeaderboardCache();

        configureFlushTask(plugin);
    }

    private static void loadPlayerFiles(EzAfk plugin) {
        if (timesDirectory == null || !timesDirectory.exists()) {
            return;
        }

        FilenameFilter yamlFilter = (dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml");
        File[] files = timesDirectory.listFiles(yamlFilter);
        if (files == null) {
            return;
        }

        for (File file : files) {
            String name = file.getName();
            if (name.length() <= 4) {
                continue;
            }

            String uuidPart = name.substring(0, name.length() - 4);
            try {
                UUID uuid = UUID.fromString(uuidPart);
                FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                long seconds = configuration.getLong("total", 0L);
                if (seconds > 0L) {
                    totalAfkSeconds.put(uuid, seconds);
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid AFK time file name detected: " + name);
            }
        }
    }

    private static void migrateLegacyData(EzAfk plugin, File legacyFile) {
        if (legacyFile == null || !legacyFile.exists()) {
            return;
        }

        FileConfiguration configuration = YamlConfiguration.loadConfiguration(legacyFile);
        if (configuration.isConfigurationSection("totals")) {
            for (String key : configuration.getConfigurationSection("totals").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long seconds = configuration.getLong("totals." + key, 0L);
                    if (seconds > 0L) {
                        totalAfkSeconds.put(uuid, seconds);
                        if (!persistPlayer(uuid, seconds, plugin)) {
                            queueSave(uuid, seconds);
                        }
                    }
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Invalid UUID found in afk-times.yml: " + key);
                }
            }
        }

        if (!legacyFile.delete()) {
            plugin.getLogger().warning("Unable to delete legacy afk-times.yml after migration.");
        }
    }

    public static void saveAll() {
        if (timesDirectory == null) {
            return;
        }

        EzAfk plugin = EzAfk.getInstance();
        if (plugin == null) {
            return;
        }

        flushPending(plugin, true);
    }

    public static void shutdown() {
        EzAfk plugin = EzAfk.getInstance();
        stopFlushTask();
        if (plugin == null) {
            return;
        }

        flushPending(plugin, true);
    }

    public static void recordAfkSession(UUID playerId, long startMillis, long endMillis) {
        if (playerId == null || startMillis <= 0L) {
            return;
        }

        long durationMillis = endMillis - startMillis;
        if (durationMillis <= 0L) {
            return;
        }

        long durationSeconds = durationMillis / 1000L;
        if (durationSeconds <= 0L) {
            return;
        }

        addAfkDuration(playerId, durationSeconds);
    }

    public static void flushActiveSessions(Map<UUID, Long> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : sessions.entrySet()) {
            recordAfkSession(entry.getKey(), entry.getValue(), now);
        }
    }

    public static long getTotalAfkSeconds(UUID playerId) {
        return totalAfkSeconds.getOrDefault(playerId, 0L);
    }

    public static List<Map.Entry<UUID, Long>> getTopPlayers(int limit) {
        if (limit <= 0) {
            return snapshotLeaderboard(leaderboardCache.size());
        }

        if (limit <= leaderboardCache.size()) {
            return snapshotLeaderboard(limit);
        }

        return buildSortedEntries(limit);
    }

    private static void addAfkDuration(UUID playerId, long seconds) {
        if (playerId == null || seconds <= 0L) {
            return;
        }

        long updatedTotal = totalAfkSeconds.merge(playerId, seconds, Long::sum);
        updateLeaderboardEntry(playerId, updatedTotal);
        queueSave(playerId, updatedTotal);
    }

    private static boolean savePlayer(UUID playerId, long total, EzAfk plugin) {
        if (playerId == null || timesDirectory == null) {
            return false;
        }

        if (total <= 0L) {
            removeFromLeaderboard(playerId);
            File playerFile = new File(timesDirectory, playerId.toString() + ".yml");
            if (playerFile.exists() && !playerFile.delete() && plugin != null) {
                plugin.getLogger().warning("Unable to delete AFK time file for player " + playerId);
                return false;
            }
            dirtyPlayers.remove(playerId);
            return true;
        }

        if (persistPlayer(playerId, total, plugin)) {
            dirtyPlayers.remove(playerId);
            return true;
        }

        dirtyPlayers.add(playerId);
        return false;
    }

    private static void queueSave(UUID playerId, long total) {
        if (playerId == null) {
            return;
        }

        pendingSaves.put(playerId, total);
        dirtyPlayers.add(playerId);
    }

    private static void flushPending(EzAfk plugin, boolean synchronous) {
        if (plugin == null) {
            return;
        }

        Map<UUID, Long> snapshot;
        synchronized (pendingSaves) {
            if (pendingSaves.isEmpty()) {
                return;
            }
            snapshot = new HashMap<>(pendingSaves);
            pendingSaves.clear();
        }

        if (synchronous) {
            persistSnapshot(snapshot, plugin);
        } else {
            CompletableFuture.runAsync(() -> persistSnapshot(snapshot, plugin));
        }
    }

    private static void persistSnapshot(Map<UUID, Long> snapshot, EzAfk plugin) {
        for (Map.Entry<UUID, Long> entry : snapshot.entrySet()) {
            if (!savePlayer(entry.getKey(), entry.getValue(), plugin)) {
                queueSave(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void configureFlushTask(EzAfk plugin) {
        if (plugin == null) {
            return;
        }

        long intervalSeconds = plugin.getConfig().getLong("afk.storage.flush-interval-seconds", DEFAULT_FLUSH_INTERVAL_SECONDS);
        if (intervalSeconds <= 0L) {
            intervalSeconds = DEFAULT_FLUSH_INTERVAL_SECONDS;
        }

        flushIntervalTicks = Math.max(20L, intervalSeconds * 20L);

        flushTask = new BukkitRunnable() {
            @Override
            public void run() {
                flushPending(plugin, false);
            }
        }.runTaskTimerAsynchronously(plugin, flushIntervalTicks, flushIntervalTicks);
    }

    private static void stopFlushTask() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
    }

    private static boolean persistPlayer(UUID playerId, long total, EzAfk plugin) {
        if (playerId == null || timesDirectory == null) {
            return false;
        }

        if (!timesDirectory.exists() && !timesDirectory.mkdirs()) {
            if (plugin != null) {
                plugin.getLogger().warning("Unable to create afk-times directory for AFK time storage.");
            }
            return false;
        }

        File playerFile = new File(timesDirectory, playerId.toString() + ".yml");
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("total", total);

        try {
            configuration.save(playerFile);
            return true;
        } catch (IOException exception) {
            if (plugin != null) {
                plugin.getLogger().warning("Unable to save AFK time for player " + playerId + ": " + exception.getMessage());
            }
            return false;
        }
    }

    private static void rebuildLeaderboardCache() {
        clearLeaderboardCache();
        for (Map.Entry<UUID, Long> entry : totalAfkSeconds.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0L) {
                updateLeaderboardEntry(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void clearLeaderboardCache() {
        leaderboardCache.clear();
        leaderboardEntries.clear();
    }

    private static List<Map.Entry<UUID, Long>> snapshotLeaderboard(int limit) {
        int effectiveLimit = limit;
        if (effectiveLimit <= 0 || effectiveLimit > leaderboardCache.size()) {
            effectiveLimit = leaderboardCache.size();
        }

        List<Map.Entry<UUID, Long>> results = new ArrayList<>(effectiveLimit);
        int count = 0;
        for (LeaderboardEntry entry : leaderboardCache) {
            if (count >= effectiveLimit) {
                break;
            }
            results.add(Map.entry(entry.playerId, entry.totalSeconds));
            count++;
        }

        return results;
    }

    private static List<Map.Entry<UUID, Long>> buildSortedEntries(int limit) {
        List<Map.Entry<UUID, Long>> entries = new ArrayList<>(totalAfkSeconds.entrySet());
        entries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        if (limit <= 0 || limit >= entries.size()) {
            return entries;
        }

        return new ArrayList<>(entries.subList(0, limit));
    }

    private static void updateLeaderboardEntry(UUID playerId, long total) {
        LeaderboardEntry existing = leaderboardEntries.remove(playerId);
        if (existing != null) {
            leaderboardCache.remove(existing);
        }

        if (total <= 0L) {
            return;
        }

        LeaderboardEntry entry = new LeaderboardEntry(playerId, total);
        leaderboardCache.add(entry);
        leaderboardEntries.put(playerId, entry);

        if (leaderboardCache.size() > LEADERBOARD_CACHE_SIZE) {
            LeaderboardEntry removed = leaderboardCache.pollLast();
            if (removed != null) {
                leaderboardEntries.remove(removed.playerId);
            }
        }
    }

    private static void removeFromLeaderboard(UUID playerId) {
        LeaderboardEntry existing = leaderboardEntries.remove(playerId);
        if (existing != null) {
            leaderboardCache.remove(existing);
        }
    }

    private record LeaderboardEntry(UUID playerId, long totalSeconds) {
    }
}

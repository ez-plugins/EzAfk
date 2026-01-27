package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
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
    
    private static BukkitTask flushTask;
    private static long flushIntervalTicks = 20L * 30L;
    private static final long DEFAULT_FLUSH_INTERVAL_SECONDS = 30L;
    private static java.io.File timesDirectory = null;
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

        migrateLegacyData(plugin, new File(dataFolder, "afk-times.yml"));

        // Load from configured storage repository if available (preferred)
        if (Registry.get().getStorageRepository() != null) {
            try {
                java.util.Map<UUID, Long> stored = Registry.get().getStorageRepository().loadAll();
                if (stored != null) {
                    totalAfkSeconds.putAll(stored);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load AFK times from storage repository: " + e.getMessage());
            }
        } else {
            plugin.getLogger().warning("No storage repository configured; AFK times will be kept in-memory only.");
        }

        rebuildLeaderboardCache();

        configureFlushTask(plugin);
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

        EzAfk plugin = Registry.get().getPlugin();
        if (plugin == null) {
            return;
        }

        // Persist via storage repository when available
        if (Registry.get().getStorageRepository() != null) {
            try {
                flushPending(plugin, true);
                Registry.get().getStorageRepository().saveAll();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save all AFK times to storage repository: " + e.getMessage());
            }
            return;
        }

        flushPending(plugin, true);
    }

    public static void shutdown() {
        EzAfk plugin = Registry.get().getPlugin();
        stopFlushTask();
        if (plugin == null) {
            return;
        }
        flushPending(plugin, true);

        if (Registry.get().getStorageRepository() != null) {
            try {
                Registry.get().getStorageRepository().shutdown();
            } catch (Exception ignored) {}
        }
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
        Long v = totalAfkSeconds.get(playerId);
        if (v != null) return v;

        // Fallback: if not cached, query configured storage repository
        if (Registry.get() != null && Registry.get().getStorageRepository() != null && playerId != null) {
            try {
                long stored = Registry.get().getStorageRepository().getPlayerAfkTime(playerId);
                if (stored > 0L) {
                    totalAfkSeconds.put(playerId, stored);
                    updateLeaderboardEntry(playerId, stored);
                }
                return stored;
            } catch (Exception e) {
                if (Registry.get() != null) Registry.get().getLogger().warning("Failed to read AFK time from storage repository: " + e.getMessage());
            }
        }

        return 0L;
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
        if (playerId == null) return false;

        // If total is zero or less, remove/zero the record in storage
        if (total <= 0L) {
            removeFromLeaderboard(playerId);
            if (Registry.get().getStorageRepository() != null) {
                try {
                    Registry.get().getStorageRepository().deletePlayer(playerId);
                    dirtyPlayers.remove(playerId);
                    return true;
                } catch (Exception e) {
                    if (plugin != null) plugin.getLogger().warning("Failed to delete AFK time via storage repository: " + e.getMessage());
                    return false;
                }
            }
            if (plugin != null) plugin.getLogger().warning("No storage repository configured to delete AFK time for player " + playerId);
            return false;
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

        long intervalSeconds = plugin.getConfig().getLong("storage.flush-interval-seconds", DEFAULT_FLUSH_INTERVAL_SECONDS);
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
        if (playerId == null) return false;

        if (Registry.get().getStorageRepository() != null) {
            try {
                Registry.get().getStorageRepository().savePlayerAfkTime(playerId, total);
                return true;
            } catch (Exception e) {
                if (plugin != null) plugin.getLogger().warning("Failed to persist AFK time via storage repository: " + e.getMessage());
                return false;
            }
        }

        if (plugin != null) plugin.getLogger().warning("No storage repository configured; cannot persist AFK time for player " + playerId);
        return false;
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

    public static boolean resetPlayer(java.util.UUID playerId) {
        if (playerId == null) return false;

        totalAfkSeconds.remove(playerId);
        dirtyPlayers.remove(playerId);
        pendingSaves.remove(playerId);
        removeFromLeaderboard(playerId);

        if (Registry.get() != null && Registry.get().getStorageRepository() != null) {
            try {
                Registry.get().getStorageRepository().deletePlayer(playerId);
                return true;
            } catch (Exception e) {
                if (Registry.get() != null) Registry.get().getLogger().warning("Failed to delete player AFK time: " + e.getMessage());
                return false;
            }
        }

        return true;
    }

    private record LeaderboardEntry(UUID playerId, long totalSeconds) {
    }
}

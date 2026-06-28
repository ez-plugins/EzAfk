package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent bypass whitelist and blacklist backed by {@code bypass-lists.yml}.
 *
 * <ul>
 *   <li><b>Whitelist</b> – players that always bypass AFK detection regardless of the
 *       {@code afk.bypass.enabled} config value or the {@code ezafk.bypass} permission.</li>
 *   <li><b>Blacklist</b> – players that are never allowed to bypass AFK detection, even when
 *       they hold the {@code ezafk.bypass} permission (useful for ops who want to be subject
 *       to AFK detection / zone rewards).</li>
 * </ul>
 *
 * <p>Blacklist takes precedence over whitelist. Each list is backed by a {@link BypassList}
 * instance which handles data storage and YAML serialisation for that list independently.</p>
 */
public final class BypassListManager {

    private static final BypassList WHITELIST = new BypassList("whitelist");
    private static final BypassList BLACKLIST = new BypassList("blacklist");
    /** Package-private so tests in the same package can point it at a temp file. */
    static File dataFile;

    private BypassListManager() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public static void load(EzAfk plugin) {
        dataFile = new File(plugin.getDataFolder(), "bypass-lists.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException ignored) {}
        }
        loadFromFile(dataFile);
    }

    /** Package-private: load from an explicit file without needing a plugin instance (used in tests). */
    static void loadFromFile(File file) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        WHITELIST.read(cfg);
        BLACKLIST.read(cfg);
    }

    /** Package-private: clear all in-memory state and reset the data-file pointer (used in tests). */
    public static void reset() {
        WHITELIST.clear();
        BLACKLIST.clear();
        dataFile = null;
    }

    private static void save() {
        if (dataFile == null) return;
        FileConfiguration cfg = new YamlConfiguration();
        WHITELIST.write(cfg);
        BLACKLIST.write(cfg);
        try {
            cfg.save(dataFile);
        } catch (IOException ignored) {}
    }

    // ── Whitelist ─────────────────────────────────────────────────────────────

    /** @return {@code true} if the player was added (was not already present). */
    public static boolean addToWhitelist(UUID id) {
        boolean added = WHITELIST.add(id);
        if (added) save();
        return added;
    }

    /** @return {@code true} if the player was removed (was present). */
    public static boolean removeFromWhitelist(UUID id) {
        boolean removed = WHITELIST.remove(id);
        if (removed) save();
        return removed;
    }

    public static boolean isWhitelisted(UUID id) {
        return WHITELIST.contains(id);
    }

    public static Set<UUID> getWhitelist() {
        return WHITELIST.snapshot();
    }

    // ── Blacklist ─────────────────────────────────────────────────────────────

    /** @return {@code true} if the player was added (was not already present). */
    public static boolean addToBlacklist(UUID id) {
        boolean added = BLACKLIST.add(id);
        if (added) save();
        return added;
    }

    /** @return {@code true} if the player was removed (was present). */
    public static boolean removeFromBlacklist(UUID id) {
        boolean removed = BLACKLIST.remove(id);
        if (removed) save();
        return removed;
    }

    public static boolean isBlacklisted(UUID id) {
        return BLACKLIST.contains(id);
    }

    public static Set<UUID> getBlacklist() {
        return BLACKLIST.snapshot();
    }
}

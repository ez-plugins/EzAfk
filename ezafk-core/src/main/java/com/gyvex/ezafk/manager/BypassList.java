package com.gyvex.ezafk.manager;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A single named bypass list that reads from and writes to one YAML section.
 *
 * <p>Instances are owned by {@link BypassListManager}; this class is
 * package-private so callers use the manager's static API.</p>
 */
final class BypassList {

    private final String yamlKey;
    private final Set<UUID> entries = new HashSet<>();

    BypassList(String yamlKey) {
        this.yamlKey = yamlKey;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /** @return {@code true} if the id was not already present. */
    boolean add(UUID id) {
        return entries.add(id);
    }

    /** @return {@code true} if the id was present and has been removed. */
    boolean remove(UUID id) {
        return entries.remove(id);
    }

    boolean contains(UUID id) {
        return entries.contains(id);
    }

    /** Returns an immutable snapshot of the current entries. */
    Set<UUID> snapshot() {
        return Collections.unmodifiableSet(entries);
    }

    void clear() {
        entries.clear();
    }

    // ── YAML I/O ─────────────────────────────────────────────────────────────

    /**
     * Populates this list from the named section in {@code cfg}.
     * Malformed UUID strings are silently skipped.
     */
    void read(FileConfiguration cfg) {
        entries.clear();
        for (String s : cfg.getStringList(yamlKey)) {
            try {
                entries.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
    }

    /** Serialises this list's entries to the named section in {@code cfg}. */
    void write(FileConfiguration cfg) {
        List<String> out = new ArrayList<>(entries.size());
        for (UUID id : entries) {
            out.add(id.toString());
        }
        cfg.set(yamlKey, out);
    }
}

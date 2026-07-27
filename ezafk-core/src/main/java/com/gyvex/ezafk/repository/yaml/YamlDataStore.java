package com.gyvex.ezafk.repository.yaml;

import com.github.ezframework.jaloquent.store.DataStore;
import com.github.ezframework.javaquerybuilder.query.Query;
import com.github.ezframework.javaquerybuilder.query.QueryableStorage;
import com.github.ezframework.javaquerybuilder.query.condition.ConditionEntry;
import com.github.ezframework.javaquerybuilder.query.condition.Connector;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jaloquent DataStore backed by a Bukkit YAML file.
 *
 * Entries are kept in a ConcurrentHashMap (keyed by path) for fast in-memory
 * access and persisted to YAML on flush(). The path format used by Jaloquent is
 * {@code prefix/id}, which maps to the YAML section {@code prefix.id}.
 */
public final class YamlDataStore implements DataStore, QueryableStorage {

    private final File file;
    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();

    public YamlDataStore(File file) throws IOException {
        this.file = file;
        if (!file.exists()) {
            file.createNewFile();
        }
        load();
    }

    // =========================================================================
    // DataStore
    // =========================================================================

    @Override
    public void save(String path, Map<String, Object> data) throws Exception {
        store.put(path, new LinkedHashMap<>(data));
    }

    @Override
    public Optional<Map<String, Object>> load(String path) throws Exception {
        return Optional.ofNullable(store.get(path));
    }

    @Override
    public void delete(String path) throws Exception {
        store.remove(path);
    }

    @Override
    public boolean exists(String path) throws Exception {
        return store.containsKey(path);
    }

    // =========================================================================
    // QueryableStorage
    // =========================================================================

    @Override
    public List<String> query(Query q) throws Exception {
        final List<ConditionEntry> conditions = q.getConditions();
        final Integer rawLimit = q.getLimit();
        final Integer limit = (rawLimit != null && rawLimit >= 0) ? rawLimit : null;

        final List<String> results = new ArrayList<>();
        for (final Map.Entry<String, Map<String, Object>> entry : store.entrySet()) {
            if (matchesConditions(entry.getValue(), conditions)) {
                final String path = entry.getKey();
                final int slash = path.lastIndexOf('/');
                final String id = slash >= 0 ? path.substring(slash + 1) : path;
                results.add(id);
                if (limit != null && results.size() >= limit) {
                    break;
                }
            }
        }
        return results;
    }

    // =========================================================================
    // Persistence
    // =========================================================================

    /**
     * Flush all in-memory entries to the backing YAML file.
     */
    public void flush() throws IOException {
        final FileConfiguration config = new YamlConfiguration();
        for (final Map.Entry<String, Map<String, Object>> pathEntry : store.entrySet()) {
            final String path = pathEntry.getKey();
            final Map<String, Object> attrs = pathEntry.getValue();
            // "afk_times/uuid" → "afk_times.uuid" section in YAML
            final String section = path.replace('/', '.');
            for (final Map.Entry<String, Object> attr : attrs.entrySet()) {
                config.set(section + "." + attr.getKey(), attr.getValue());
            }
        }
        config.save(file);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void load() {
        final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (final String prefix : config.getKeys(false)) {
            if (!config.isConfigurationSection(prefix)) continue;
            final var prefixSection = config.getConfigurationSection(prefix);
            if (prefixSection == null) continue;
            for (final String id : prefixSection.getKeys(false)) {
                if (!prefixSection.isConfigurationSection(id)) continue;
                final var entrySection = prefixSection.getConfigurationSection(id);
                if (entrySection == null) continue;
                final Map<String, Object> data = new LinkedHashMap<>();
                for (final String key : entrySection.getKeys(false)) {
                    data.put(key, entrySection.get(key));
                }
                store.put(prefix + "/" + id, data);
            }
        }
    }

    private boolean matchesConditions(Map<String, Object> row, List<ConditionEntry> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        boolean result = false;
        boolean first = true;
        for (final ConditionEntry entry : conditions) {
            final boolean matches = entry.getCondition().matches(row, entry.getColumn());
            if (first) {
                result = matches;
                first = false;
            } else if (entry.getConnector() == Connector.OR) {
                result = result || matches;
            } else {
                result = result && matches;
            }
        }
        return result;
    }
}

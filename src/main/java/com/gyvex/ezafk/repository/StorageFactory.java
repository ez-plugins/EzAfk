package com.gyvex.ezafk.repository;

import com.gyvex.ezafk.bootstrap.Registry;

/**
 * Factory to create a StorageRepository based on configuration.
 */
public final class StorageFactory {
    private StorageFactory() {}

    public static StorageRepository create() {
        String type = "yaml";
        try {
            if (Registry.get() != null && Registry.get().getPlugin() != null) {
                type = Registry.get().getPlugin().getConfig().getString("storage.type", "yaml");
            }
        } catch (Exception ignored) {}

        if (type == null) type = "yaml";

        switch (type.toLowerCase()) {
            case "sqlite":
                try { return new com.gyvex.ezafk.repository.sqlite.SQLiteStorage(); } catch (Throwable t) { Registry.get().getLogger().warning("Failed to create sqlite storage: " + t.getMessage()); }
                break;
            case "mysql":
                try { return new com.gyvex.ezafk.repository.mysql.MySQLStorage(); } catch (Throwable t) { Registry.get().getLogger().warning("Failed to create mysql storage: " + t.getMessage()); }
                break;
            case "yaml":
            default:
                break;
        }

        // Fallback to YAML storage
        return new com.gyvex.ezafk.repository.yaml.YamlStorage();
    }
}

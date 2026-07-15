package com.gyvex.ezafk.version;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public final class VersionAdapterFactory {

    private VersionAdapterFactory() {}

    public static String detectMcVersion() {
        String version = Bukkit.getVersion();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("MC: (\\d+\\.\\d+\\.\\d+)").matcher(version);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = java.util.regex.Pattern.compile("(\\d+\\.\\d+\\.\\d+)").matcher(version);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }

    public static boolean isRegistryAccessSupported() {
        String version = detectMcVersion();
        if ("unknown".equals(version)) {
            return false;
        }
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);
            return major > 1 || (major == 1 && minor > 21) || (major == 1 && minor == 21 && patch >= 1);
        } catch (Exception e) {
            return false;
        }
    }

    public static VersionAdapter create(Plugin plugin, Logger logger) {
        if (isRegistryAccessSupported()) {
            try {
                Class<?> clazz = VersionAdapterFactory.class.getClassLoader()
                        .loadClass("com.gyvex.ezafk.version.Paper261PlusVersionAdapter");
                java.lang.reflect.Constructor<?> ctor = clazz.getDeclaredConstructor();
                return (VersionAdapter) ctor.newInstance();
            } catch (ClassNotFoundException e) {
                logger.warning("[EzAfk] Paper261PlusVersionAdapter not found, using fallback");
            } catch (Exception e) {
                logger.warning("[EzAfk] Failed to load Paper261PlusVersionAdapter: " + e.getMessage());
            }
        }

        return new FallbackVersionAdapter();
    }
}
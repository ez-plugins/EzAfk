package com.gyvex.ezafk.compatibility.platform;

/**
 * Detects the server platform at runtime so the rest of the codebase can
 * branch between Bukkit/Paper and Folia without duplicating the check.
 */
public final class PlatformDetector {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    private PlatformDetector() {}

    /** Returns {@code true} when the server is running Folia. */
    public static boolean isFolia() {
        return FOLIA;
    }
}

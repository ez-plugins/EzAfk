package com.gyvex.ezafk.integration.worldguard.flag;

import com.gyvex.ezafk.integration.worldguard.FlagRegistrar;

import java.util.logging.Logger;

/**
 * Holds the AFK bypass flag instance without statically referencing WorldGuard
 * classes. The flag is created lazily via reflection when needed so that the
 * plugin can safely load when WorldGuard is not present on the classpath.
 */
public final class AfkBypassFlag {
    private static volatile Object FLAG = null; // actual type: com.sk89q.worldguard.protection.flags.StateFlag

    private AfkBypassFlag() {}

    public static Object get() {
        if (FLAG == null) {
            try {
                Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
                try {
                    FLAG = stateFlagClass.getConstructor(String.class, boolean.class).newInstance("afk-bypass", false);
                } catch (NoSuchMethodException nsme) {
                    // Fallback to Boolean boxed constructor if present
                    FLAG = stateFlagClass.getConstructor(String.class, Boolean.class).newInstance("afk-bypass", Boolean.FALSE);
                }
            } catch (Throwable t) {
                // WorldGuard not present or failed to construct; leave FLAG null
                FLAG = null;
            }
        }
        return FLAG;
    }

    public static void set(Object flag) {
        if (flag != null) FLAG = flag;
    }

    /**
     * Ensure registered via reflection-friendly registrar.
     */
    public static Object ensureRegistered(Object registry, Logger logger, String mode) {
        Object active = FlagRegistrar.registerStateFlag(FLAG, registry, logger, mode);
        if (active != null) set(active);
        return active;
    }
}

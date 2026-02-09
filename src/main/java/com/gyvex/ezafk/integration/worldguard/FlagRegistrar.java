package com.gyvex.ezafk.integration.worldguard;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection-based helper for registering WorldGuard flags. Uses raw Object
 * parameters to avoid linking WorldGuard classes when WorldGuard is absent.
 */
public final class FlagRegistrar {
    private FlagRegistrar() {}

    public static Object registerStateFlag(Object candidate, Object registry, Logger logger, String mode) {
        if (candidate == null || registry == null) return null;

        try {
            // registry.register(candidate);
            try {
                registry.getClass().getMethod("register", candidate.getClass()).invoke(registry, candidate);
            } catch (NoSuchMethodException nsme) {
                // try generic Object param
                try {
                    registry.getClass().getMethod("register", Object.class).invoke(registry, candidate);
                } catch (NoSuchMethodException ignored) {
                    // unable to call register
                }
            }

            // candidate.getName()
            String name = tryInvokeGetName(candidate);
            logger.info("Registered WorldGuard flag: " + name);
            return candidate;
        } catch (Throwable t) {
            // handle conflict or locked registry
            try {
                String name = tryInvokeGetName(candidate);
                Object existing = tryInvokeGet(registry, name);
                if (existing != null && isInstanceOfStateFlag(existing)) {
                    logger.info("Using existing WorldGuard flag: " + name);
                    return existing;
                }

                if (t instanceof IllegalStateException) {
                    if (mode != null && "manual".equalsIgnoreCase(mode.trim())) {
                        logger.info("WorldGuard flag registration configured as 'manual'; skipping automatic registration for: " + name);
                        return null;
                    }

                    logger.warning("WorldGuard flag registry is locked; could not register flag: " + name);
                    logger.warning("Remediation: ensure EzAfk loads before WorldGuard or set integration.flag-registration: manual in config.");
                    return null;
                }

                logger.log(Level.WARNING, "Unexpected error while registering WorldGuard flag: " + name, t);
            } catch (Throwable inner) {
                logger.log(Level.WARNING, "Unexpected error while registering WorldGuard flag.", t);
            }
            return null;
        }
    }

    private static String tryInvokeGetName(Object candidate) {
        try {
            Object res = candidate.getClass().getMethod("getName").invoke(candidate);
            return res == null ? "<unknown>" : res.toString();
        } catch (Throwable t) {
            return "<unknown>";
        }
    }

    private static Object tryInvokeGet(Object registry, String name) {
        try {
            return registry.getClass().getMethod("get", String.class).invoke(registry, name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isInstanceOfStateFlag(Object obj) {
        try {
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            return stateFlagClass.isInstance(obj);
        } catch (Throwable t) {
            return false;
        }
    }
}

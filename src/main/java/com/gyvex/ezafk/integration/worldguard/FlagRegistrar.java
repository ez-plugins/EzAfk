package com.gyvex.ezafk.integration.worldguard;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class FlagRegistrar {
    private FlagRegistrar() {}

    /**
     * Attempt to register the provided StateFlag into the given FlagRegistry.
     * If a flag with the same name already exists and is a StateFlag it will be returned.
     * On irrecoverable failure this returns null.
     *
     * @param candidate the StateFlag to register
     * @param registry the WorldGuard FlagRegistry
     * @param logger plugin logger for messages
     * @param mode either "auto" or "manual" to control logging behaviour
     * @return the active StateFlag (either the candidate or an existing one), or null on failure
     */
    public static StateFlag registerStateFlag(StateFlag candidate, FlagRegistry registry, Logger logger, String mode) {
        if (candidate == null || registry == null) return null;
        try {
            registry.register(candidate);
            logger.info("Registered WorldGuard flag: " + candidate.getName());
            return candidate;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(candidate.getName());
            if (existing instanceof StateFlag) {
                logger.info("Using existing WorldGuard flag: " + candidate.getName());
                return (StateFlag) existing;
            } else {
                logger.log(Level.SEVERE, "Flag name conflict in WorldGuard for: " + candidate.getName(), e);
                return null;
            }
        } catch (IllegalStateException e) {
            // Registry locked
            Flag<?> existing = registry.get(candidate.getName());
            if (existing instanceof StateFlag) {
                logger.info("Using existing WorldGuard flag (registry locked): " + candidate.getName());
                return (StateFlag) existing;
            }

            if (mode != null && "manual".equalsIgnoreCase(mode.trim())) {
                logger.info("WorldGuard flag registration configured as 'manual'; skipping automatic registration for: " + candidate.getName());
                return null;
            }

            logger.warning("WorldGuard flag registry is locked; could not register flag: " + candidate.getName());
            logger.warning("Remediation: ensure EzAfk loads before WorldGuard or set integration.flag-registration: manual in config.");
            return null;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Unexpected error while registering WorldGuard flag: " + candidate.getName(), t);
            return null;
        }
    }
}

package com.gyvex.ezafk.integration.worldguard.flag;

import com.gyvex.ezafk.integration.worldguard.FlagRegistrar;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import java.util.logging.Logger;

public final class AfkBypassFlag {
    private static StateFlag FLAG = new StateFlag("afk-bypass", false);

    private AfkBypassFlag() {}

    public static StateFlag get() {
        return FLAG;
    }

    public static void set(StateFlag flag) {
        if (flag != null) FLAG = flag;
    }

    /**
     * Ensure the AFK BYPASS flag is registered in the provided registry. Returns the active flag or null.
     */
    public static StateFlag ensureRegistered(FlagRegistry registry, Logger logger, String mode) {
        StateFlag active = FlagRegistrar.registerStateFlag(FLAG, registry, logger, mode);
        if (active != null) set(active);
        return active;
    }
}

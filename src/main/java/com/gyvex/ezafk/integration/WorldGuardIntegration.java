package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class WorldGuardIntegration extends Integration {
    public static StateFlag AFK_BYPASS = new StateFlag("afk-bypass", false);

    public static boolean isInAfkBypassSection(Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regions == null) {
            return false;
        }

        ApplicableRegionSet set = regions.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));

        for (ProtectedRegion region : set) {
            if (region.getFlags().containsKey(AFK_BYPASS) &&
                    region.getFlag(AFK_BYPASS) == StateFlag.State.ALLOW) {
                return true;
            }
        }

        return false;
    }

    public void setupTags() {
        EzAfk plugin = Registry.get().getPlugin();

        if (!plugin.getConfig().getBoolean("integration.worldguard")) {
            return;
        }

        plugin.getLogger().info("WorldGuard integration is enabled");

        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

            try {
                registry.register(AFK_BYPASS);
                plugin.getLogger().info("AFK BYPASS flag registered in WorldGuard");
                this.isSetup = true;
            } catch (FlagConflictException e) {
                Flag<?> existing = registry.get(AFK_BYPASS.getName());

                if (existing instanceof StateFlag) {
                    AFK_BYPASS = (StateFlag) existing;
                    plugin.getLogger().info("Using existing AFK BYPASS flag from WorldGuard");
                    this.isSetup = true;
                } else {
                    plugin.getLogger().log(
                            Level.SEVERE,
                            "Something went wrong while registering WorldGuard flag " + AFK_BYPASS.getName(),
                            e
                    );
                }
            } catch (IllegalStateException e) {
                // WorldGuard's flag registry is locked (typically after WorldGuard initialization).
                // Try to reuse an existing flag if present; otherwise skip registration quietly.
                Flag<?> existing = registry.get(AFK_BYPASS.getName());

                if (existing instanceof StateFlag) {
                    AFK_BYPASS = (StateFlag) existing;
                    plugin.getLogger().info("Using existing AFK BYPASS flag from WorldGuard (registry locked)");
                    this.isSetup = true;
                } else {
                    plugin.getLogger().warning("WorldGuard flag registry is locked; cannot register AFK BYPASS flag now. Skipping WorldGuard flag registration.");
                }
            }
        } catch (NoClassDefFoundError ex) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "WorldGuard API not available. Skipping WorldGuard integration setup.",
                    ex
            );
        }
    }

    @Override
    public void load() {
        // Detect WorldGuard API presence at load time so callers can decide whether to proceed.
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
            Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            this.isSetup = true;
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            this.isSetup = false;
        }
    }

    @Override
    public void unload() {
    }
}

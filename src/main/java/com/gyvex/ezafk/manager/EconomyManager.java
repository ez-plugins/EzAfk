package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.integration.EconomyIntegration;
import com.gyvex.ezafk.integration.Integration;
import com.gyvex.ezafk.state.AfkState;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public final class EconomyManager {
    private static final Map<UUID, Long> nextRecurringCharge = new HashMap<>();
    private static final Map<UUID, Long> blockedUntil = new HashMap<>();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

    private EconomyManager() {
    }

    public static void reset() {
        nextRecurringCharge.clear();
        blockedUntil.clear();
    }

    public static void onActivity(Player player) {
        if (player == null) {
            return;
        }

        blockedUntil.remove(player.getUniqueId());
    }

    public static void onDisable(UUID playerId) {
        if (playerId == null) {
            return;
        }

        nextRecurringCharge.remove(playerId);
        blockedUntil.remove(playerId);
    }

    public static boolean isEconomyBlocked(Player player) {
        if (player == null) {
            return false;
        }

        if (!isEconomyActive()) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        Long until = blockedUntil.get(playerId);

        if (until == null) {
            return false;
        }

        if (System.currentTimeMillis() >= until) {
            blockedUntil.remove(playerId);
            return false;
        }

        return true;
    }

    public static boolean handleEnter(Player player, boolean initiatedByPlayer) {
        if (player == null) {
            return false;
        }

        EzAfk plugin = Registry.get().getPlugin();

        if (!isEconomyActive() || bypassesEconomy(player)) {
            scheduleNextCharge(player, plugin);
            return true;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("economy.cost.enter");

        if (section == null || !section.getBoolean("enabled", false)) {
            scheduleNextCharge(player, plugin);
            return true;
        }

        double amount = section.getDouble("amount", 0D);

        if (amount <= 0) {
            scheduleNextCharge(player, plugin);
            return true;
        }

        Economy economy = getEconomy();

        if (economy == null) {
            return true;
        }

        boolean requireFunds = section.getBoolean("require-funds", true);

        boolean hasFunds = economy.has(player, amount);

        if (!hasFunds) {
            if (requireFunds) {
                if (initiatedByPlayer) {
                    sendEconomyMessage(player, "economy.enter.insufficient", "&cYou need %amount% to go AFK.", amount, economy);
                }
                handleFailure(player, initiatedByPlayer);
                return false;
            }

            logSkippedCharge(plugin, player, "insufficient funds");

            sendEconomyMessage(player, "economy.enter.skipped", "&eThe AFK entry fee was skipped because your account couldn't be charged.", amount, economy);

            scheduleNextCharge(player, plugin);
            return true;
        }

        EconomyResponse response = economy.withdrawPlayer(player, amount);

        if (!response.transactionSuccess()) {
            if (requireFunds) {
                if (initiatedByPlayer) {
                    sendEconomyMessage(player, "economy.enter.failed", "&cWe couldn't charge your account to go AFK.", amount, economy);
                }
                handleFailure(player, initiatedByPlayer);
                return false;
            }

            String reason = response.errorMessage == null || response.errorMessage.isBlank()
                    ? "unknown Vault error"
                    : response.errorMessage;
            logSkippedCharge(plugin, player, "Vault withdrawal failed: " + reason);

            sendEconomyMessage(player, "economy.enter.skipped", "&eThe AFK entry fee was skipped because your account couldn't be charged.", amount, economy);

            scheduleNextCharge(player, plugin);
            return true;
        }

        sendEconomyMessage(player, "economy.enter.charged", "&eYou paid %amount% to go AFK.", amount, economy);
        scheduleNextCharge(player, plugin);
        return true;
    }

    public static void processRecurringCharges() {
        if (!isEconomyActive() || !isRecurringEnabled()) {
            return;
        }

        EzAfk plugin = Registry.get().getPlugin();

        if (plugin == null) {
            return;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("economy.cost.recurring");

        if (section == null) {
            return;
        }

        double amount = section.getDouble("amount", 0D);

        if (amount <= 0) {
            return;
        }

        long interval = Math.max(1L, section.getLong("interval", 60L)) * 1000L;
        boolean requireFunds = section.getBoolean("require-funds", true);
        boolean kickOnFail = section.getBoolean("kick-on-fail", false);

        Economy economy = getEconomy();

        if (economy == null) {
            return;
        }

        long now = System.currentTimeMillis();

        for (UUID playerId : new HashSet<>(AfkState.afkPlayers)) {
            Player player = Bukkit.getPlayer(playerId);

            if (player == null) {
                continue;
            }

            if (bypassesEconomy(player)) {
                continue;
            }

            long scheduled = nextRecurringCharge.computeIfAbsent(playerId, id -> now + interval);

            if (now < scheduled) {
                continue;
            }

            nextRecurringCharge.put(playerId, now + interval);

            boolean hasFunds = economy.has(player, amount);

            if (!hasFunds) {
                if (requireFunds) {
                    sendEconomyMessage(player, "economy.recurring.revoked-insufficient", "&cYou don't have enough funds to remain AFK, so your AFK status has been removed.", amount, economy);
                    AfkState.disable(plugin, player);
                } else {
                    sendEconomyMessage(player, "economy.recurring.skipped", "&eThe recurring AFK fee was skipped because your account couldn't be charged. We'll try again later.", amount, economy);
                    if (kickOnFail) {
                        AfkState.disable(plugin, player);
                    } else {
                        blockPlayer(player, plugin);
                    }
                }
                continue;
            }

            EconomyResponse response = economy.withdrawPlayer(player, amount);

            if (!response.transactionSuccess()) {
                if (requireFunds) {
                    sendEconomyMessage(player, "economy.recurring.revoked-failed", "&cWe couldn't charge your account to remain AFK, so your AFK status has been removed.", amount, economy);
                    AfkState.disable(plugin, player);
                } else {
                    sendEconomyMessage(player, "economy.recurring.failed", "&cWe couldn't charge your account to remain AFK. We'll try again later.", amount, economy);
                    if (kickOnFail) {
                        AfkState.disable(plugin, player);
                    } else {
                        blockPlayer(player, plugin);
                    }
                }
                continue;
            }

            sendEconomyMessage(player, "economy.recurring.charged", "&eYou paid %amount% to remain AFK.", amount, economy);
        }
    }

    private static void blockPlayer(Player player, EzAfk plugin) {
        long retryDelay = Math.max(0L, plugin.getConfig().getLong("economy.cost.enter.retry-delay", 60L));

        if (retryDelay <= 0L) {
            blockedUntil.remove(player.getUniqueId());
            return;
        }

        blockedUntil.put(player.getUniqueId(), System.currentTimeMillis() + retryDelay * 1000L);
    }

    private static void handleFailure(Player player, boolean initiatedByPlayer) {
        EzAfk plugin = Registry.get().getPlugin();

        if (!initiatedByPlayer && plugin != null) {
            blockPlayer(player, plugin);
        }
    }

    private static void logSkippedCharge(EzAfk plugin, Player player, String reason) {
        if (plugin == null || player == null) {
            return;
        }

        plugin.getLogger().fine(() -> "Skipping AFK entry charge for " + player.getName() + ": " + reason);
    }

    private static void scheduleNextCharge(Player player, EzAfk plugin) {
        if (player == null || plugin == null) {
            return;
        }

        if (!isRecurringEnabled()) {
            nextRecurringCharge.remove(player.getUniqueId());
            return;
        }

        long interval = Math.max(1L, plugin.getConfig().getLong("economy.cost.recurring.interval", 60L)) * 1000L;
        nextRecurringCharge.put(player.getUniqueId(), System.currentTimeMillis() + interval);
    }

    private static boolean bypassesEconomy(Player player) {
        EzAfk plugin = Registry.get().getPlugin();

        if (plugin == null) {
            return false;
        }

        // Use AfkBypassFlag to check WorldGuard regions directly instead of delegating
        // to WorldGuardIntegration. Avoid compile-time references to WorldGuard
        // classes by using reflection so the plugin can load without WorldGuard.
        try {
            Object flag = com.gyvex.ezafk.integration.worldguard.flag.AfkBypassFlag.get();
            if (flag != null) {
                // WorldGuard.getInstance().getPlatform().getRegionContainer();
                Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Object wgInstance = worldGuardClass.getMethod("getInstance").invoke(null);
                Object platform = wgInstance.getClass().getMethod("getPlatform").invoke(wgInstance);
                Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);

                // adapter for world -> region manager
                Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                Object adaptedWorld = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null, player.getWorld());

                Object regions = container.getClass().getMethod("get", adaptedWorld.getClass()).invoke(container, adaptedWorld);
                if (regions != null) {
                    Object blockVector = bukkitAdapterClass.getMethod("asBlockVector", org.bukkit.Location.class).invoke(null, player.getLocation());
                    Object set = regions.getClass().getMethod("getApplicableRegions", blockVector.getClass()).invoke(regions, blockVector);
                    // Iterate over set (it is Iterable<ProtectedRegion>)
                    for (Object region : (Iterable<?>) set) {
                        try {
                            Object flagsMap = region.getClass().getMethod("getFlags").invoke(region);
                            boolean contains = (boolean) flagsMap.getClass().getMethod("containsKey", Object.class).invoke(flagsMap, flag);
                            if (contains) {
                                Object flagVal = region.getClass().getMethod("getFlag", flag.getClass()).invoke(region, flag);
                                // Compare to StateFlag.State.ALLOW
                                Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
                                Class<?> stateEnum = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag$State");
                                Object allow = java.lang.Enum.valueOf((Class<Enum>) stateEnum, "ALLOW");
                                if (allow.equals(flagVal)) {
                                    return true;
                                }
                            }
                        } catch (NoSuchMethodException nsme) {
                            // older/newer API mismatch; ignore and continue
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // WorldGuard not available or error while checking regions; fall through.
        }

        String permission = plugin.getConfig().getString("economy.bypass-permission", "");

        if (permission == null || permission.isBlank()) {
            return false;
        }

        return player.hasPermission(permission);
    }

    private static boolean isEconomyActive() {
        EzAfk plugin = Registry.get().getPlugin();

        if (plugin == null) {
            return false;
        }

        if (!plugin.getConfig().getBoolean("economy.enabled")) {
            return false;
        }

        return IntegrationManager.hasIntegration("economy");
    }

    private static boolean isRecurringEnabled() {
        EzAfk plugin = Registry.get().getPlugin();

        if (plugin == null) {
            return false;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("economy.cost.recurring");
        return section != null && section.getBoolean("enabled", false);
    }

    private static Economy getEconomy() {
        if (!IntegrationManager.hasIntegration("economy")) {
            return null;
        }

        Integration integration = IntegrationManager.getIntegration("economy");

        if (integration instanceof EconomyIntegration economyIntegration) {
            return economyIntegration.getEconomy();
        }

        return null;
    }

    private static void sendEconomyMessage(Player player, String path, String fallback, double amount, Economy economy) {
        if (player == null) {
            return;
        }

        String formattedAmount = economy != null ? economy.format(amount) : DECIMAL_FORMAT.format(amount);

        MessageManager.sendMessage(player, path, fallback, Map.of("amount", formattedAmount));
    }
}

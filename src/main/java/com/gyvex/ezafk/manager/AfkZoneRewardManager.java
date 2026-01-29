package com.gyvex.ezafk.manager;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.manager.MessageManager;
import com.gyvex.ezafk.integration.Integration;
import com.gyvex.ezafk.integration.EconomyIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.gyvex.ezafk.zone.Zone;
import net.milkbowl.vault.economy.Economy;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class AfkZoneRewardManager {
    private static final Map<UUID, Map<String, RewardState>> states = new HashMap<>();

    private AfkZoneRewardManager() {}

    public static void processRewards() {
        EzAfk plugin = Registry.get().getPlugin();
        if (plugin == null) return;

        long now = System.currentTimeMillis();

        for (UUID playerId : new HashSet<>(AfkState.afkPlayers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) continue;

            Zone zone = AfkZoneManager.getZoneForPlayer(player);
            if (zone == null || !zone.rewardEnabled || zone.rewardIntervalSeconds <= 0 || zone.rewardAmount <= 0.0) {
                // clear any saved state for this player/zone
                Map<String, RewardState> m = states.get(playerId);
                if (m != null && zone != null) m.remove(zone.name);
                continue;
            }

            Map<String, RewardState> playerStates = states.computeIfAbsent(playerId, k -> new HashMap<>());
            RewardState rs = playerStates.get(zone.name);

            if (rs == null) {
                rs = new RewardState(now + zone.rewardIntervalSeconds * 1000L, 0, 0, 0L);
                playerStates.put(zone.name, rs);
                continue; // first interval scheduled
            }

            if (now < rs.nextScheduled) {
                continue; // not yet
            }

            // If a per-player limit is configured, check cooldown and reset if expired
            if (zone.rewardLimit > 0) {
                if (rs.limitCooldownUntil > now) {
                    // still in cooldown, notify player and schedule next check
                    long remainingMs = rs.limitCooldownUntil - now;
                    String timeStr = formatDurationMillis(remainingMs);
                    try {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("reward_limit", String.valueOf(zone.rewardLimit));
                        ph.put("reward_name", zone.name == null ? "" : zone.name);
                        ph.put("time_remaining", timeStr);
                        MessageManager.sendMessage(player, "afkzone.reward.limit.cooldown", "&cYou already reached the %reward_limit% for %reward_name%, come back in %time_remaining%.", ph);
                    } catch (Throwable ignored) {}
                    rs.nextScheduled = now + Math.max(1000L, zone.rewardIntervalSeconds * 1000L);
                    continue;
                } else {
                    // cooldown expired - reset count
                    if (rs.grantCount >= zone.rewardLimit) {
                        rs.grantCount = 0;
                        rs.limitCooldownUntil = 0L;
                    }
                }
            }

            long intervalMs = Math.max(1L, zone.rewardIntervalSeconds) * 1000L;
            long elapsed = now - rs.nextScheduled;
            long intervalsPassed = 1 + (elapsed / intervalMs);

            int toGrant = (int) Math.min(intervalsPassed, Integer.MAX_VALUE);
            if (zone.rewardMaxStack > 0) {
                // cap by max stack
                toGrant = Math.min(toGrant, Math.max(0, zone.rewardMaxStack - rs.stackCount));
            }
            // cap by per-player limit if configured
            if (zone.rewardLimit > 0) {
                int remaining = Math.max(0, zone.rewardLimit - rs.grantCount);
                toGrant = Math.min(toGrant, remaining);
            }

            if (toGrant <= 0) {
                // nothing to grant (stack full or limit reached)
                if (zone.rewardLimit > 0 && rs.grantCount >= zone.rewardLimit) {
                    // set cooldown if configured
                    if (zone.rewardLimitCooldownSeconds > 0) {
                        rs.limitCooldownUntil = now + zone.rewardLimitCooldownSeconds * 1000L;
                    }
                    // notify player
                    try {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("reward_limit", String.valueOf(zone.rewardLimit));
                        ph.put("reward_name", zone.name == null ? "" : zone.name);
                        ph.put("time_remaining", formatDurationMillis(Math.max(0, rs.limitCooldownUntil - now)));
                        MessageManager.sendMessage(player, "afkzone.reward.limit.reached", "&cYou already reached the %reward_limit% for %reward_name%, come back in %time_remaining%.", ph);
                    } catch (Throwable ignored) {}
                }
                rs.nextScheduled = now + intervalMs; // schedule next check
                continue;
            }

            // Try to grant rewards (economy deposit)
            Economy econ = null;
            if (IntegrationManager.hasIntegration("economy")) {
                try {
                    Integration integration = (Integration) IntegrationManager.getIntegration("economy");
                    if (integration instanceof EconomyIntegration ei) {
                        econ = ei.getEconomy();
                    }
                } catch (Throwable ignored) {}
            }

            boolean grantSuccess = false;
            String grantFailReason = null;
            String rewardType = zone.rewardType == null ? "economy" : zone.rewardType.toLowerCase();

            if ("economy".equals(rewardType)) {
                double totalAmount = zone.rewardAmount * toGrant;
                if (econ != null) {
                    try {
                        net.milkbowl.vault.economy.EconomyResponse resp = econ.depositPlayer(player, totalAmount);
                        grantSuccess = resp.transactionSuccess();
                        if (!grantSuccess) {
                            grantFailReason = resp.errorMessage == null ? "transaction-failed" : "transaction-failed: " + resp.errorMessage;
                        }
                    } catch (Throwable ex) {
                        grantSuccess = false;
                        grantFailReason = "exception: " + ex.getClass().getSimpleName();
                    }
                } else {
                    plugin.getLogger().log(Level.FINE, "No economy provider available for AFK zone rewards (zone=" + zone.name + ")");
                    grantSuccess = false;
                    grantFailReason = "no-economy-provider";
                }
                if (grantSuccess) {
                    plugin.getLogger().log(Level.FINE, "Granted AFK zone economy reward to " + player.getName() + " zone=" + zone.name + " amount=" + (zone.rewardAmount * toGrant) + " (" + toGrant + ")");
                }
            } else if ("command".equals(rewardType)) {
                // Execute configured command as console, replacing %player% and %amount%
                if (zone.rewardCommand != null && !zone.rewardCommand.isBlank()) {
                    for (int i = 0; i < toGrant; i++) {
                        String cmd = zone.rewardCommand.replace("%player%", player.getName());
                        cmd = cmd.replace("%amount%", String.valueOf(zone.rewardAmount));
                        try {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        } catch (Throwable ex) {
                            plugin.getLogger().log(Level.FINE, "Failed to execute AFK zone command reward: " + cmd);
                        }
                    }
                    grantSuccess = true;
                    plugin.getLogger().log(Level.FINE, "Executed AFK zone command reward for " + player.getName() + " zone=" + zone.name + " (" + toGrant + ")");
                }
            } else if ("item".equals(rewardType)) {
                if (zone.rewardItemMaterial != null && !zone.rewardItemMaterial.isBlank()) {
                    try {
                        Material mat = Material.matchMaterial(zone.rewardItemMaterial);
                        if (mat != null) {
                            int totalItems = zone.rewardItemAmount * toGrant;
                            ItemStack stack = new ItemStack(mat, Math.max(1, Math.min(64, totalItems)));
                            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
                            // If leftover, drop on ground
                            if (!leftover.isEmpty()) {
                                for (ItemStack s : leftover.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), s);
                                }
                            }
                            grantSuccess = true;
                            plugin.getLogger().log(Level.FINE, "Granted AFK zone item reward to " + player.getName() + " zone=" + zone.name + " item=" + zone.rewardItemMaterial + " count=" + totalItems);
                        } else {
                            plugin.getLogger().log(Level.FINE, "Invalid material for AFK zone item reward: " + zone.rewardItemMaterial);
                        }
                    } catch (Throwable ex) {
                        plugin.getLogger().log(Level.FINE, "Failed to grant AFK zone item reward: " + ex.getMessage());
                    }
                }
            } else {
                plugin.getLogger().log(Level.FINE, "Unknown reward type for AFK zone: " + rewardType);
            }

                if (grantSuccess) {
                    rs.stackCount += toGrant;
                    rs.grantCount += toGrant;
                    rs.nextScheduled = now + intervalMs;

                    // If grant count reached limit, start cooldown and notify
                    if (zone.rewardLimit > 0 && rs.grantCount >= zone.rewardLimit) {
                        if (zone.rewardLimitCooldownSeconds > 0) {
                            rs.limitCooldownUntil = now + zone.rewardLimitCooldownSeconds * 1000L;
                        }
                        try {
                            Map<String, String> ph = new HashMap<>();
                            ph.put("reward_limit", String.valueOf(zone.rewardLimit));
                            ph.put("reward_name", zone.name == null ? "" : zone.name);
                            ph.put("time_remaining", formatDurationMillis(Math.max(0, rs.limitCooldownUntil - now)));
                            MessageManager.sendMessage(player, "afkzone.reward.limit.reached", "&cYou already reached the %reward_limit% for %reward_name%, come back in %time_remaining%.", ph);
                        } catch (Throwable ignored) {}
                    }

                    // Send localized player message about the reward
                    try {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("zone", zone.name == null || zone.name.isEmpty() ? "" : zone.name);
                        if ("economy".equals(rewardType)) {
                            DecimalFormat df = new DecimalFormat("0.##");
                            String formatted = df.format(zone.rewardAmount * toGrant);
                            placeholders.put("amount", formatted);
                            MessageManager.sendMessage(player, "afkzone.reward.granted.economy", "&aYou received %amount% for staying AFK in %zone%.", placeholders);
                        } else if ("command".equals(rewardType)) {
                            placeholders.put("amount", String.valueOf(zone.rewardAmount));
                            MessageManager.sendMessage(player, "afkzone.reward.granted.command", "&aYou received a command reward for staying AFK in %zone%.", placeholders);
                        } else if ("item".equals(rewardType)) {
                            placeholders.put("item", zone.rewardItemMaterial == null ? "" : zone.rewardItemMaterial);
                            placeholders.put("count", String.valueOf(zone.rewardItemAmount * toGrant));
                            MessageManager.sendMessage(player, "afkzone.reward.granted.item", "&aYou received %count%x %item% for staying AFK in %zone%.", placeholders);
                        } else {
                            MessageManager.sendMessage(player, "afkzone.reward.granted", "&aYou received a reward for staying AFK in %zone%.", placeholders);
                        }
                    } catch (Throwable ignored) {
                    }
                } else {
                    rs.nextScheduled = now + intervalMs; // schedule next attempt
                    // If the player is an operator, notify them about the failure to aid debugging
                    try {
                        if (player.isOp()) {
                            Map<String, String> ph = new HashMap<>();
                            ph.put("zone", zone.name == null ? "" : zone.name);
                            ph.put("reason", grantFailReason == null ? "unknown" : grantFailReason);
                            MessageManager.sendMessage(player, "afkzone.reward.failed", "&cAFK zone reward failed for %zone%: %reason%", ph);
                        }
                    } catch (Throwable ignored) {}
                }
        }
    }

    private static final class RewardState {
        long nextScheduled;
        int stackCount;

        int grantCount;
        long limitCooldownUntil;

        RewardState(long nextScheduled, int stackCount, int grantCount, long limitCooldownUntil) {
            this.nextScheduled = nextScheduled;
            this.stackCount = stackCount;
            this.grantCount = grantCount;
            this.limitCooldownUntil = limitCooldownUntil;
        }
    }

    public static void resetPlayerCounts(java.util.UUID playerId) {
        Map<String, RewardState> m = states.get(playerId);
        if (m != null) {
            for (RewardState rs : m.values()) {
                rs.grantCount = 0;
                rs.limitCooldownUntil = 0L;
            }
        }
    }

    public static void resetPlayerCountForZone(java.util.UUID playerId, String zoneName) {
        Map<String, RewardState> m = states.get(playerId);
        if (m != null) {
            RewardState rs = m.get(zoneName);
            if (rs != null) {
                rs.grantCount = 0;
                rs.limitCooldownUntil = 0L;
            }
        }
    }

    private static String formatDurationMillis(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000;
        long hours = s / 3600;
        long minutes = (s % 3600) / 60;
        long seconds = s % 60;
        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        return String.format("%ds", seconds);
    }
}

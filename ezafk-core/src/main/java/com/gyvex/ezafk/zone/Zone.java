package com.gyvex.ezafk.zone;

import org.bukkit.entity.Player;

import java.util.List;

public final class Zone {
    public final String name;
    public final String world;
    public final double minX, minY, minZ, maxX, maxY, maxZ;
    public final boolean rewardEnabled;
    public final long rewardIntervalSeconds;
    public final int rewardMaxStack; // -1 for unlimited
    public final double rewardAmount;
    public final String rewardType; // economy | command | item
    public final String rewardCommand; // for command type
    public final String rewardItemMaterial; // for item type
    public final int rewardItemAmount;
    public final int rewardLimit; // per-player limit
    public final long rewardLimitCooldownSeconds;
    /** Whether EzCountdown reward countdown notification is enabled for this zone. */
    public final boolean notificationEnabled;
    /**
     * EzCountdown display types for the reward countdown notification
     * (e.g. ACTION_BAR, TITLE, BOSS_BAR). An empty list disables EzCountdown
     * and falls back to the regular chat/action-bar grant message.
     */
    public final List<String> notificationDisplays;
    /**
     * Message shown in the EzCountdown countdown. Supports EzCountdown's live
     * {seconds} / {formatted} placeholders and EzAfk's %zone% / %amount%.
     */
    public final String notificationMessage;
    /**
     * How many seconds the countdown should run. Defaults to
     * {@code rewardIntervalSeconds} when ≤ 0, so the countdown ticks from
     * the interval duration down to zero and the player knows exactly when
     * the next reward lands.
     */
    public final int notificationDuration;

    public Zone(String name, String world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                boolean rewardEnabled, long rewardIntervalSeconds, int rewardMaxStack, double rewardAmount,
                String rewardType, String rewardCommand, String rewardItemMaterial, int rewardItemAmount,
                int rewardLimit, long rewardLimitCooldownSeconds,
                boolean notificationEnabled, List<String> notificationDisplays,
                String notificationMessage, int notificationDuration) {
        this.name = name == null ? "" : name;
        this.world = world;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.rewardEnabled = rewardEnabled;
        this.rewardIntervalSeconds = rewardIntervalSeconds;
        this.rewardMaxStack = rewardMaxStack;
        this.rewardAmount = rewardAmount;
        this.rewardType = rewardType == null ? "economy" : rewardType;
        this.rewardCommand = rewardCommand;
        this.rewardItemMaterial = rewardItemMaterial;
        this.rewardItemAmount = Math.max(1, rewardItemAmount);
        this.rewardLimit = rewardLimit;
        this.rewardLimitCooldownSeconds = rewardLimitCooldownSeconds;
        this.notificationEnabled = notificationEnabled;
        this.notificationDisplays = notificationDisplays == null ? List.of() : List.copyOf(notificationDisplays);
        this.notificationMessage = notificationMessage == null ? "&7Next reward in &e{seconds}s &7in &a%zone%" : notificationMessage;
        this.notificationDuration = notificationDuration;
    }

    public boolean contains(Player player) {
        if (!player.getWorld().getName().equals(this.world)) return false;
        double x = player.getLocation().getX();
        double y = player.getLocation().getY();
        double z = player.getLocation().getZ();

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}

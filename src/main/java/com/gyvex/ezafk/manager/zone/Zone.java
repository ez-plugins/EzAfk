package com.gyvex.ezafk.manager.zone;

import org.bukkit.entity.Player;

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

    public Zone(String name, String world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, boolean rewardEnabled, long rewardIntervalSeconds, int rewardMaxStack, double rewardAmount, String rewardType, String rewardCommand, String rewardItemMaterial, int rewardItemAmount) {
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

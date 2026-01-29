package com.gyvex.ezafk.listener;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.manager.MessageManager;
import com.gyvex.ezafk.state.AfkActivationMode;
import com.gyvex.ezafk.state.AfkReason;
import com.gyvex.ezafk.state.AfkState;
import com.gyvex.ezafk.state.LastActiveState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MoveListener implements Listener {
    private final EzAfk plugin;
    private final Set<UUID> vehicleBypassHandled = new HashSet<>();
    private final Set<UUID> infiniteWaterFlowHandled = new HashSet<>();
    private final Set<UUID> bubbleColumnHandled = new HashSet<>();

    public MoveListener(EzAfk plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        boolean antiVehicle = this.plugin.getConfig().getBoolean("afk.anti.infinite-vehicle");
        boolean flagOnly = this.plugin.getConfig().getBoolean("afk.anti.flag-only");

        if (antiVehicle && player.isInsideVehicle()) {
            if (flagOnly) {
                if (vehicleBypassHandled.add(playerId)) {
                    markBypass(player, AfkReason.ANTI_VEHICLE,
                            "Vehicle movement detected at " + formatLocation(player.getLocation()));
                }
            } else {
                if (vehicleBypassHandled.add(playerId)) {
                    MessageManager.sendMessage(player, "anti.vehicle",
                            "&cYou cannot AFK while inside a vehicle.");
                }
                if (player.isInsideVehicle()) {
                    player.leaveVehicle();
                }
            }
            return;
        }

        boolean antiInfiniteWaterFlow = this.plugin.getConfig().getBoolean("afk.anti.infinite-waterflow");

        if (antiInfiniteWaterFlow && isInInfiniteWaterFlow(player)) {
            if (flagOnly) {
                if (infiniteWaterFlowHandled.add(playerId)) {
                    markBypass(player, AfkReason.ANTI_INFINITE_WATER,
                            "Water flow detected at " + formatLocation(player.getLocation()));
                }
            } else {
                if (infiniteWaterFlowHandled.add(playerId)) {
                    MessageManager.sendMessage(player, "anti.infinite-waterflow",
                            "&cYou cannot use water flow to avoid AFK detection.");
                }
            }
            return;
        }

        boolean antiBubbleColumn = this.plugin.getConfig().getBoolean("afk.anti.bubble-column");

        if (antiBubbleColumn && isInBubbleColumn(player)) {
            if (flagOnly) {
                if (bubbleColumnHandled.add(playerId)) {
                    markBypass(player, AfkReason.ANTI_BUBBLE_COLUMN,
                            "Bubble column detected at " + formatLocation(player.getLocation()));
                }
            } else {
                if (bubbleColumnHandled.add(playerId)) {
                    MessageManager.sendMessage(player, "anti.bubble-column",
                            "&cYou cannot use bubble columns to avoid AFK detection.");
                }
            }
            return;
        }

        if (!hasMeaningfulPositionChange(event)) {
            // Ignore orientation-only updates so AFK detection only reacts to real movement.
            return;
        }

        LastActiveState.update(player);
        vehicleBypassHandled.remove(playerId);
        infiniteWaterFlowHandled.remove(playerId);

        if (AfkState.isAfk(playerId)) {
            AfkState.disable(this.plugin, player);
        }
    }

    private void markBypass(Player player, AfkReason reason, String detail) {
        AfkState.markAfk(this.plugin, player, reason, detail, AfkActivationMode.SILENT);
    }

    private String formatLocation(Location location) {
        if (location.getWorld() == null) {
            return String.format("(%.1f, %.1f, %.1f)",
                    location.getX(), location.getY(), location.getZ());
        }

        return String.format("%s (%.1f, %.1f, %.1f)",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ());
    }

    private boolean hasMeaningfulPositionChange(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return false;
        }

        if (from.getWorld() != to.getWorld()) {
            return true;
        }

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        double distanceSquared = dx * dx + dy * dy + dz * dz;

        return distanceSquared > 1.0E-4;
    }

    private boolean isInInfiniteWaterFlow(Player player) {
        Block feetBlock = player.getLocation().getBlock();
        Block bodyBlock = feetBlock.getRelative(BlockFace.UP);

        return isFlowingWaterThatPushes(feetBlock) || isFlowingWaterThatPushes(bodyBlock);
    }

    private boolean isFlowingWaterThatPushes(Block block) {
        if (block.getType() != Material.WATER) {
            return false;
        }

        Block below = block.getRelative(BlockFace.DOWN);
        if (below.getType() == Material.BUBBLE_COLUMN) {
            return false;
        }

        if (!(block.getBlockData() instanceof Levelled levelled)) {
            return false;
        }

        int level = levelled.getLevel();

        if (level == 0) {
            // Still source water; normal swimming is allowed.
            return false;
        }

        // Falling water (maximum level) should only flag if there is space to fall into.
        if (level >= levelled.getMaximumLevel()) {
            return below.isPassable();
        }

        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block relative = block.getRelative(face);

            if (relative.isPassable() && relative.getType() != Material.WATER) {
                return true;
            }

            if (relative.getType() == Material.WATER && relative.getBlockData() instanceof Levelled adjacentLevelled) {
                if (adjacentLevelled.getLevel() > level) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean isInBubbleColumn(Player player) {
        Block feet = player.getLocation().getBlock();
        Block body = feet.getRelative(BlockFace.UP);
        Block below = feet.getRelative(BlockFace.DOWN);

        return feet.getType() == Material.BUBBLE_COLUMN
                || body.getType() == Material.BUBBLE_COLUMN
                || below.getType() == Material.BUBBLE_COLUMN;
    }
}

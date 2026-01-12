package com.gyvex.ezafk.state;

/**
 * Captures why EzAfk marked a player as AFK so staff can audit the decision.
 */
public enum AfkReason {
    MANUAL("Player toggled AFK manually."),
    COMMAND_FORCED("Marked AFK by a staff command."),
    INACTIVITY("No recent player activity was detected."),
    ANTI_INFINITE_WATER("Bypass detection: sustained water flow movement."),
    ANTI_VEHICLE("Bypass detection: vehicle movement without input."),
    ANTI_BUBBLE_COLUMN("Bypass detection: bubble column movement."),
    OTHER("AFK status updated by the plugin.");

    private final String displayName;

    AfkReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

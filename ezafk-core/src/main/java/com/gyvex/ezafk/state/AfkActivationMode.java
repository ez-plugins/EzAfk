package com.gyvex.ezafk.state;

/**
 * Controls how much feedback a player receives when they are marked as AFK.
 */
public enum AfkActivationMode {
    /**
     * Preserve the original behaviour by notifying the player and broadcasting
     * the AFK change to the server.
     */
    STANDARD,

    /**
     * Silently mark the player as AFK without alerts, used when staff want to
     * review suspicious behaviour without tipping the player off.
     */
    SILENT
}

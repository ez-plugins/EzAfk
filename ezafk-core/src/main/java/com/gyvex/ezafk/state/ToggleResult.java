package com.gyvex.ezafk.state;

/**
 * Represents the outcome of attempting to toggle a player's AFK state.
 */
public enum ToggleResult {
    /** The player has been marked as AFK. */
    NOW_AFK,
    /** The player is no longer marked as AFK. */
    NO_LONGER_AFK,
    /** The AFK state was not changed (for example due to economy restrictions). */
    FAILED
}

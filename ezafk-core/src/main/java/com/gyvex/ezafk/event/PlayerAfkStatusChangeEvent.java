package com.gyvex.ezafk.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import com.gyvex.ezafk.state.AfkReason;

/**
 * Custom event fired when a player's AFK status changes in EzAfk.
 * Other plugins can listen to this event to react to AFK/return actions.
 */
public class PlayerAfkStatusChangeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final boolean isAfk;
    private final AfkReason reason;
    private final String detail;
    private boolean cancelled;

    /**
     * @param player The player whose AFK status changed
     * @param isAfk True if the player is now AFK, false if they returned
     * @param reason The reason for AFK status change
     * @param detail Additional details about the change
     */
    public PlayerAfkStatusChangeEvent(Player player, boolean isAfk, AfkReason reason, String detail) {
        this.player = player;
        this.isAfk = isAfk;
        this.reason = reason;
        this.detail = detail;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isAfk() {
        return isAfk;
    }

    public AfkReason getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

package com.gyvex.ezafk.state;

/**
 * Stores metadata for why a player is currently considered AFK.
 */
public record AfkStatusDetails(AfkReason reason, String detail) {
    public AfkStatusDetails {
        if (reason == null) {
            reason = AfkReason.OTHER;
        }
    }

    public String getReasonDisplayName() {
        return reason.getDisplayName();
    }

    public boolean hasDetail() {
        return detail != null && !detail.isBlank();
    }
}

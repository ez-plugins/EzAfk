package com.gyvex.ezafk.state;

/**
 * Stores metadata for why a player is currently considered AFK.
 */
public final class AfkStatusDetails {
    private final AfkReason reason;
    private final String detail;

    public AfkStatusDetails(AfkReason reason, String detail) {
        this.reason = (reason == null) ? AfkReason.OTHER : reason;
        this.detail = detail;
    }

    public AfkReason reason() {
        return reason;
    }

    public String detail() {
        return detail;
    }

    public String getReasonDisplayName() {
        return reason.getDisplayName();
    }

    public boolean hasDetail() {
        return detail != null && !detail.isBlank();
    }

    public String getDetail() {
        return detail;
    }
}

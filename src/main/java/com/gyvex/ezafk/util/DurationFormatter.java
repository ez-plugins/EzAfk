package com.gyvex.ezafk.util;

import java.time.Duration;

public final class DurationFormatter {
    private DurationFormatter() {
    }

    public static String formatDuration(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }

        Duration duration = Duration.ofSeconds(seconds);

        long days = duration.toDays();
        duration = duration.minusDays(days);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();
        duration = duration.minusMinutes(minutes);
        long remainingSeconds = duration.getSeconds();

        StringBuilder builder = new StringBuilder();

        if (days > 0) {
            builder.append(days).append("d ");
        }

        if (hours > 0 || builder.length() > 0) {
            builder.append(hours).append("h ");
        }

        if (minutes > 0 || builder.length() > 0) {
            builder.append(minutes).append("m ");
        }

        builder.append(remainingSeconds).append("s");

        return builder.toString().trim();
    }
}

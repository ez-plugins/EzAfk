package com.gyvex.ezafk.state;

import com.gyvex.ezafk.manager.EconomyManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class LastActiveState {
    public static HashMap<UUID, Long> lastActive = new HashMap<>();

    public static void update(Player player) {
        lastActive.put(player.getUniqueId(), System.currentTimeMillis());
        EconomyManager.onActivity(player);
    }

    public static long getLastActive(Player player) {
        return getLastActive(player.getUniqueId());
    }

    public static long getLastActive(UUID playerId) {
        return lastActive.getOrDefault(playerId, System.currentTimeMillis());
    }

    public static long getSecondsSinceLastActive(UUID playerId) {
        long last = getLastActive(playerId);
        long diff = System.currentTimeMillis() - last;
        if (diff < 0) {
            return 0L;
        }

        return diff / 1000L;
    }
}

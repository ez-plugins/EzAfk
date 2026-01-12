package com.gyvex.ezafk.integration;

import java.util.UUID;
import org.bukkit.entity.Player;

interface PlayerListNameAdapter {
    void removeInvalidEntries();

    String getBaseName(Player player);

    boolean apply(Player player, String targetName);

    boolean restore(UUID uuid);

    void restoreAll();
}

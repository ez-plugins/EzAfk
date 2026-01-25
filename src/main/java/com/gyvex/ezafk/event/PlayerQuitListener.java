package com.gyvex.ezafk.event;

import com.gyvex.ezafk.manager.EconomyManager;
import com.gyvex.ezafk.manager.AfkTimeManager;
import com.gyvex.ezafk.state.AfkState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        UUID playerId = player.getUniqueId();
        AfkTimeManager.flushActiveSessions(java.util.Map.of(playerId, System.currentTimeMillis()));

        if (!AfkState.afkPlayers.contains(playerId)) {
            AfkState.forgetDisplayName(playerId);
            EconomyManager.onDisable(playerId);
            return;
        }

        AfkState.afkPlayers.remove(playerId);
        AfkState.forgetDisplayName(playerId);
        EconomyManager.onDisable(playerId);
        // Clear any stored AFK zone pos1/pos2 for this player
        com.gyvex.ezafk.command.EzAfkCommand.clearPositions(playerId);
    }
}

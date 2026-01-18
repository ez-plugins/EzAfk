package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.event.PlayerAfkStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listens for AFK status changes and triggers Simple Voice Chat sound playback.
 */
public class SimpleVoiceChatAfkListener implements Listener {
    private final SimpleVoiceChatIntegration svcIntegration;

    public SimpleVoiceChatAfkListener(EzAfk plugin) {
        this.svcIntegration = new SimpleVoiceChatIntegration(plugin);
    }

    @EventHandler
    public void onAfkStatusChange(PlayerAfkStatusChangeEvent event) {
        if (event.isAfk()) {
            svcIntegration.playAfkSound(event.getPlayer());
        }
    }
}

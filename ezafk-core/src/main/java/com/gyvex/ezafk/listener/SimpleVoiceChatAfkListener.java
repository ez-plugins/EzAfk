package com.gyvex.ezafk.listener;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.event.PlayerAfkStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listens for AFK status changes and triggers Simple Voice Chat sound playback.
 */
public class SimpleVoiceChatAfkListener implements Listener {
    private final com.gyvex.ezafk.integration.voicechat.SimpleVoiceChatIntegration svcIntegration;

    public SimpleVoiceChatAfkListener(EzAfk plugin) {
        this.svcIntegration = new com.gyvex.ezafk.integration.voicechat.SimpleVoiceChatIntegration(plugin);
    }

    @EventHandler
    public void onAfkStatusChange(PlayerAfkStatusChangeEvent event) {
        if (event.isAfk()) {
            svcIntegration.playAfkSound(event.getPlayer());
        } else {
            svcIntegration.playReturnSound(event.getPlayer());
        }
    }
}

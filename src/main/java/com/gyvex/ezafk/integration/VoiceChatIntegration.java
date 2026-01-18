package com.gyvex.ezafk.integration;

import org.bukkit.plugin.java.JavaPlugin;
import com.gyvex.ezafk.integration.EzAfkVoicechatServiceRegistrar;

/**
 * Integration wrapper for Simple Voice Chat registration via IntegrationManager.
 */
public class VoiceChatIntegration extends Integration {
    private final JavaPlugin plugin;

    public VoiceChatIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        EzAfkVoicechatServiceRegistrar.register(plugin);
    }

    @Override
    public void unload() {
        // No specific unload actions required for Voice Chat integration
    }
}

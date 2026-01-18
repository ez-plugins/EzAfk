package com.gyvex.ezafk.integration;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.bukkit.plugin.java.JavaPlugin;

public class EzAfkVoicechatServiceRegistrar {
    public static void register(JavaPlugin plugin) {
        BukkitVoicechatService service = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            service.registerPlugin(new EzAfkVoicechatPlugin());
        }
    }
}

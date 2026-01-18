package com.gyvex.ezafk.integration;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

public class EzAfkVoicechatPlugin implements VoicechatPlugin {
    private static VoicechatServerApi api;

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }
    
    @Override
    public String getPluginId() {
        return "ezafk";
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        api = event.getVoicechat();
    }

    public static VoicechatServerApi getApi() {
        return api;
    }
}

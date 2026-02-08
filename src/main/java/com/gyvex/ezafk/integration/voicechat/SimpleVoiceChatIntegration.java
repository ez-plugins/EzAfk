package com.gyvex.ezafk.integration.voicechat;

import java.io.InputStream;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.bootstrap.Registry;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;

import org.bukkit.entity.Player;

/**
 * Handles integration with Simple Voice Chat to play custom sounds for AFK events.
 */
public class SimpleVoiceChatIntegration {
    private final EzAfk plugin;

    public SimpleVoiceChatIntegration(EzAfk plugin) {
        this.plugin = plugin;
    }

    /**
     * Plays the AFK sound for the specified player using Simple Voice Chat.
     * This is a stub. You must implement the actual call to Simple Voice Chat's API.
     */
    public void playAfkSound(Player player) {
        if (!Registry.get().getConfigManager().isAfkSoundEnabled()) return;
        String soundFile = Registry.get().getConfigManager().getAfkSoundFile();
        java.io.File mp3File = new java.io.File(Registry.get().getPlugin().getDataFolder(), soundFile); // plugins/EzAfk/mp3/ezafk-sound.mp3
        if (!mp3File.exists()) {
            Registry.get().getLogger().warning("AFK sound file not found: " + mp3File.getAbsolutePath());
            return;
        }
        java.io.InputStream mp3Stream;
        try {
            mp3Stream = new java.io.FileInputStream(mp3File);
        } catch (Exception e) {
            Registry.get().getLogger().warning("Failed to open AFK sound file: " + e.getMessage());
            return;
        }

        // Get VoicechatServerApi instance
        VoicechatServerApi api = EzAfkVoicechatPlugin.getApi();
        if (api == null) return;

        // Get the VoicechatConnection for the player
        VoicechatConnection connection = api.getConnectionOf(player.getUniqueId());
        if (connection == null) return;

        // Decode MP3 to 48kHz 16-bit PCM
        short[] audio;
        try {
            audio = decodeMp3ToPcm(mp3Stream);
        } catch (Exception e) {
            Registry.get().getLogger().warning("Failed to decode AFK sound: " + e.getMessage());
            return;
        }
        if (audio == null || audio.length == 0) return;

        // Create a StaticAudioChannel for the player using the recommended API
        java.util.UUID channelId = java.util.UUID.randomUUID();
        // For Bukkit/Spigot, use api.fromServerLevel(player.getWorld()) which returns the correct type
        StaticAudioChannel channel = api.createStaticAudioChannel(channelId, api.fromServerLevel(player.getWorld()), connection);
        if (channel == null) return;

        // Play the audio
        AudioPlayer audioPlayer = api.createAudioPlayer(channel, api.createEncoder(), audio);
        audioPlayer.startPlaying();
    }

    /**
     * Plays the return-from-AFK sound for the specified player using Simple Voice Chat.
     */
    public void playReturnSound(Player player) {
        if (!Registry.get().getConfigManager().isUnafkSoundEnabled()) return;
        String soundFile = Registry.get().getConfigManager().getUnafkSoundFile();
        java.io.File mp3File = new java.io.File(Registry.get().getPlugin().getDataFolder(), soundFile);
        if (!mp3File.exists()) {
            Registry.get().getLogger().warning("Return AFK sound file not found: " + mp3File.getAbsolutePath());
            return;
        }
        java.io.InputStream mp3Stream;
        try {
            mp3Stream = new java.io.FileInputStream(mp3File);
        } catch (Exception e) {
            Registry.get().getLogger().warning("Failed to open return AFK sound file: " + e.getMessage());
            return;
        }

        VoicechatServerApi api = EzAfkVoicechatPlugin.getApi();
        if (api == null) return;

        VoicechatConnection connection = api.getConnectionOf(player.getUniqueId());
        if (connection == null) return;

        short[] audio;
        try {
            audio = decodeMp3ToPcm(mp3Stream);
        } catch (Exception e) {
            Registry.get().getLogger().warning("Failed to decode return AFK sound: " + e.getMessage());
            return;
        }
        if (audio == null || audio.length == 0) return;

        java.util.UUID channelId = java.util.UUID.randomUUID();
        StaticAudioChannel channel = api.createStaticAudioChannel(channelId, api.fromServerLevel(player.getWorld()), connection);
        if (channel == null) return;

        AudioPlayer audioPlayer = api.createAudioPlayer(channel, api.createEncoder(), audio);
        audioPlayer.startPlaying();
    }

    /**
     * Decodes an MP3 file to 48kHz 16-bit PCM samples (short[]).
     * You may use a library like Tritonus, JLayer, or JavaSound for actual decoding.
     * This is a stub and must be implemented for production use.
     */
    private short[] decodeMp3ToPcm(InputStream mp3Stream) throws Exception {
        // Implementation using JLayer (javazoom)
        // Requires JLayer dependency: 'javazoom:jlayer:1.0.1'
        try (java.io.BufferedInputStream bis = new java.io.BufferedInputStream(mp3Stream)) {
            javazoom.jl.decoder.Bitstream bitstream = new javazoom.jl.decoder.Bitstream(bis);
            javazoom.jl.decoder.Header header;
            java.util.ArrayList<Short> pcmList = new java.util.ArrayList<>();
            javazoom.jl.decoder.Decoder decoder = new javazoom.jl.decoder.Decoder();
            while ((header = bitstream.readFrame()) != null) {
                javazoom.jl.decoder.SampleBuffer output = (javazoom.jl.decoder.SampleBuffer) decoder.decodeFrame(header, bitstream);
                // Resample to 48000 Hz if needed (JLayer outputs 44100 Hz by default)
                short[] samples = output.getBuffer();
                int sampleRate = output.getSampleFrequency();
                if (sampleRate != 48000) {
                    samples = resample(samples, sampleRate, 48000);
                }
                for (short s : samples) {
                    pcmList.add(s);
                }
                bitstream.closeFrame();
            }
            // Convert to short[]
            short[] pcm = new short[pcmList.size()];
            for (int i = 0; i < pcmList.size(); i++) {
                pcm[i] = pcmList.get(i);
            }
            return pcm;
        }
    }

    /**
     * Resamples a PCM short[] array from srcRate to dstRate using linear interpolation.
     */
    private short[] resample(short[] input, int srcRate, int dstRate) {
        if (srcRate == dstRate) return input;
        int srcLen = input.length;
        int dstLen = (int) (((long) srcLen * dstRate) / srcRate);
        short[] output = new short[dstLen];
        for (int i = 0; i < dstLen; i++) {
            float srcIndex = ((float) i * srcRate) / dstRate;
            int idx = (int) srcIndex;
            float frac = srcIndex - idx;
            short s1 = input[Math.min(idx, srcLen - 1)];
            short s2 = input[Math.min(idx + 1, srcLen - 1)];
            output[i] = (short) (s1 + frac * (s2 - s1));
        }
        return output;
    }
}

package com.gyvex.ezafk.compatibility.sound;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class SoundCompat {

    private SoundCompat() {}

    /**
     * Plays a sound for a player, trying each provided name in order until one succeeds.
     * First attempts {@link Sound#valueOf} (modern enum lookup); if none of the names
     * match a known enum constant the first non-empty name is played via the string-based
     * {@code playSound} reflection path for legacy / custom sound support.
     *
     * @param player     The target player.
     * @param volume     Sound volume.
     * @param pitch      Sound pitch.
     * @param soundNames One or more sound name candidates (modern name first).
     */
    public static void playSound(Player player, float volume, float pitch, String... soundNames) {
        if (player == null || soundNames == null) return;

        for (String soundName : soundNames) {
            if (soundName == null || soundName.isEmpty()) continue;
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
                return;
            } catch (IllegalArgumentException ignored) {}
        }

        // Fallback: string-based playSound (covers custom resource-pack sounds)
        for (String soundName : soundNames) {
            if (soundName == null || soundName.isEmpty()) continue;
            try {
                Method playSound = player.getClass().getMethod(
                        "playSound", player.getLocation().getClass(), String.class,
                        float.class, float.class);
                playSound.invoke(player, player.getLocation(), soundName, volume, pitch);
                return;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
        }
    }
}

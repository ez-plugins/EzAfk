package com.gyvex.ezafk.compatibility;

import org.bukkit.Particle;

public class ParticleCompat {
    public static Particle getHappyVillager() {
        try {
            return Particle.valueOf("HAPPY_VILLAGER");
        } catch (IllegalArgumentException e) {
            return Particle.VILLAGER_HAPPY; // legacy fallback
        }
    }

    public static Particle getBlock() {
        try {
            return Particle.valueOf("BLOCK");
        } catch (IllegalArgumentException e) {
            return Particle.BLOCK_CRACK; // legacy fallback
        }
    }

    public static Particle getCherryLeaves() {
        try {
            return Particle.valueOf("CHERRY_LEAVES");
        } catch (IllegalArgumentException e) {
            return null; // not available in older versions
        }
    }

    public static Particle getSmoke() {
        try {
            return Particle.valueOf("SMOKE");
        } catch (IllegalArgumentException e) {
            return Particle.SMOKE_NORMAL; // legacy fallback
        }
    }
}

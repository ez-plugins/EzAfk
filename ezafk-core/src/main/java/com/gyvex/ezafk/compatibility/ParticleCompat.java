package com.gyvex.ezafk.compatibility;

import org.bukkit.Particle;

public class ParticleCompat {
    public static Particle getHappyVillager() {
        try {
            return Particle.valueOf("HAPPY_VILLAGER");
        } catch (IllegalArgumentException e) {
            try {
                return Enum.valueOf(Particle.class, "VILLAGER_HAPPY");
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }

    public static Particle getBlock() {
        try {
            return Particle.valueOf("BLOCK");
        } catch (IllegalArgumentException e) {
            try {
                return Enum.valueOf(Particle.class, "BLOCK_CRACK");
            } catch (IllegalArgumentException e2) {
                return null;
            }
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
            try {
                return Enum.valueOf(Particle.class, "SMOKE_NORMAL");
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }
}

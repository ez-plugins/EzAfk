package com.gyvex.ezafk.state;

import org.bukkit.Material;
import org.bukkit.Particle;
import com.gyvex.ezafk.compatibility.CompatibilityUtil;
import com.gyvex.ezafk.compatibility.ParticleCompat;
import org.bukkit.entity.Player;

public class StateAnimator {
    public static void playAfkEnableAnimation(Player player) {
        Particle happyVillager = ParticleCompat.getHappyVillager();

        if (happyVillager != null) {
            player.getWorld().spawnParticle(happyVillager, player.getLocation(), 20, 1, 1, 1, 0.1);
        }

        Particle blockParticle = ParticleCompat.getBlock();
        if (blockParticle != null) {
            player.getWorld().spawnParticle(blockParticle, player.getLocation(), 30, 1, 1, 1, 1, Material.OAK_LOG.createBlockData());
        }

        Particle cherryLeaves = ParticleCompat.getCherryLeaves();

        if (cherryLeaves != null) {
            player.getWorld().spawnParticle(cherryLeaves, player.getLocation().add(0, 2, 0), 20, 1, 1, 1, 0.1);
        }

        CompatibilityUtil.playSound(player, 1.0f, 1.0f, "BLOCK_WOOD_BREAK", "WOOD_BREAK", "DIG_WOOD");
        CompatibilityUtil.playSound(player, 0.5f, 1.5f, "ENTITY_EXPERIENCE_ORB_PICKUP", "ORB_PICKUP", "ENTITY_ORB_PICKUP");
    }

    public static void playAfkDisableAnimation(Player player) {
        Particle smoke = ParticleCompat.getSmoke();

        if (smoke != null) {
            player.getWorld().spawnParticle(smoke, player.getLocation(), 20, 1, 1, 1, 0.1);
        }

        CompatibilityUtil.playSound(player, 1.0f, 1.0f, "BLOCK_GRASS_BREAK", "DIG_GRASS", "STEP_GRASS");
        CompatibilityUtil.playSound(player, 0.5f, 0.5f, "BLOCK_WOOD_HIT", "WOOD_CLICK", "STEP_WOOD");
    }
}

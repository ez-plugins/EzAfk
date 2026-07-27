package com.gyvex.ezafk.version;

import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.logging.Logger;

final class FallbackVersionAdapter implements VersionAdapter {

    private final Attribute fallbackAttribute;
    private final Logger logger = Logger.getLogger("EzAfk");

    FallbackVersionAdapter() {
        Attribute attr = null;
        try {
            attr = Attribute.valueOf("GENERIC_MAX_HEALTH");
        } catch (Exception e) {
            try {
                attr = Attribute.valueOf("MAX_HEALTH");
            } catch (Exception ignored) {}
        }
        fallbackAttribute = attr;
        if (fallbackAttribute == null) {
            logger.warning("[EzAfk] Could not resolve MAX_HEALTH attribute");
        }
    }

    @Override
    public Attribute getMaxHealthAttribute() {
        return fallbackAttribute;
    }

    @Override
    public PotionEffectType getEffect(String minecraftKey) {
        try {
            return PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft(minecraftKey));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public Enchantment getEnchantment(String minecraftKey) {
        try {
            return Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(minecraftKey));
        } catch (Exception ignored) {
            return null;
        }
    }
}
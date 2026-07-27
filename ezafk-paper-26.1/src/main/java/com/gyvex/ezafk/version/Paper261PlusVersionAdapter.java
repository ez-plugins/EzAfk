package com.gyvex.ezafk.version;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

public final class Paper261PlusVersionAdapter implements VersionAdapter {

    @Override
    public Attribute getMaxHealthAttribute() {
        return Attribute.MAX_HEALTH;
    }

    @Override
    public PotionEffectType getEffect(String minecraftKey) {
        RegistryAccess registryAccess = RegistryAccess.registryAccess();
        return registryAccess.getRegistry(RegistryKey.MOB_EFFECT).get(NamespacedKey.minecraft(minecraftKey));
    }

    @Override
    public Enchantment getEnchantment(String minecraftKey) {
        RegistryAccess registryAccess = RegistryAccess.registryAccess();
        return registryAccess.getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(minecraftKey));
    }
}
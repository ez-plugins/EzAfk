package com.gyvex.ezafk.version;

import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

public interface VersionAdapter {

    Attribute getMaxHealthAttribute();

    PotionEffectType getEffect(String minecraftKey);

    Enchantment getEnchantment(String minecraftKey);
}
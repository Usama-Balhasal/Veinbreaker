package org.ISoma05.veinBreaker.Utils;

import org.ISoma05.veinBreaker.Config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Handles XP orb spawning for vein-mined ores.
 * <p>
 * XP amounts are configurable in config.yml under the {@code xp} section.
 * Silk Touch suppresses XP, matching vanilla Minecraft behaviour.
 */
public final class XPUtils {

    private static final Random RANDOM = new Random();

    private XPUtils() {}

    /**
     * Spawns an XP orb at the given location if the ore should produce XP.
     * Silk Touch suppresses the drop entirely (vanilla parity).
     *
     * @param location    Centre of the broken ore block.
     * @param material    The ore material.
     * @param tool        The tool used to break the block.
     * @param config      ConfigManager to read XP ranges from.
     */
    public static void dropXp(Location location, Material material, ItemStack tool, ConfigManager config) {
        // Silk Touch suppresses XP (vanilla behaviour)
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) return;

        String family = getOreFamily(material);
        if (family == null) return;

        int min = config.getXpMin(family);
        int max = config.getXpMax(family);

        if (max <= 0) return; // No XP for this ore (iron, gold, copper)

        int amount = (min == max) ? min : min + RANDOM.nextInt(max - min + 1);
        if (amount <= 0) return;

        ExperienceOrb orb = location.getWorld().spawn(location, ExperienceOrb.class);
        orb.setExperience(amount);
    }

    /**
     * Maps a {@link Material} to its XP config key (matches config.yml {@code xp} section).
     * Returns {@code null} for non-ore materials.
     */
    public static String getOreFamily(Material material) {
        return switch (material) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE          -> "diamond";
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE          -> "emerald";
            case COAL_ORE,    DEEPSLATE_COAL_ORE             -> "coal";
            case LAPIS_ORE,   DEEPSLATE_LAPIS_ORE            -> "lapis";
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE        -> "redstone";
            case NETHER_QUARTZ_ORE                           -> "quartz";
            case IRON_ORE,    DEEPSLATE_IRON_ORE             -> "iron";
            case GOLD_ORE,    DEEPSLATE_GOLD_ORE             -> "gold";
            case NETHER_GOLD_ORE                             -> "nether_gold";
            case COPPER_ORE,  DEEPSLATE_COPPER_ORE           -> "copper";
            case ANCIENT_DEBRIS                              -> "ancient_debris";
            default -> null;
        };
    }
}

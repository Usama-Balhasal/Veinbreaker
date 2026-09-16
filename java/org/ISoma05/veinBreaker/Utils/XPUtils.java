package org.ISoma05.veinBreaker.Utils;

import org.ISoma05.veinBreaker.Config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Handles XP rewards for vein-mined ores, geodes, and cave blocks.
 * XP amounts are configurable in config.yml under the {@code xp} section.
 * Silk Touch suppresses XP, matching vanilla Minecraft behaviour.
 */
public final class XPUtils {

    private static final Random RANDOM = new Random();

    private XPUtils() {}

    // =========================================================================
    //  Orb-drop mode (vanilla style)
    // =========================================================================

    public static void dropXp(Location location, Material material, ItemStack tool, ConfigManager config) {
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) return;

        int amount = rollXp(material, config);
        if (amount <= 0) return;

        ExperienceOrb orb = location.getWorld().spawn(location, ExperienceOrb.class);
        orb.setExperience(amount);
    }

    // =========================================================================
    //  Auto-collect mode (direct grant)
    // =========================================================================

    public static void giveXp(Player player, Material material, ItemStack tool, ConfigManager config) {
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) return;

        int amount = rollXp(material, config);
        if (amount <= 0) return;

        player.giveExp(amount);
    }

    // =========================================================================
    //  Shared helpers
    // =========================================================================

    private static int rollXp(Material material, ConfigManager config) {
        String family = getOreFamily(material);
        if (family == null) return 0;

        int min = config.getXpMin(family);
        int max = config.getXpMax(family);

        if (max <= 0) return 0;

        return (min == max) ? min : min + RANDOM.nextInt(max - min + 1);
    }

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
            case AMETHYST_CLUSTER                            -> "amethyst_cluster";
            case SCULK                                       -> "sculk";
            case SCULK_CATALYST                              -> "sculk_catalyst";
            case SCULK_SENSOR                                -> "sculk_sensor";
            case SCULK_SHRIEKER                              -> "sculk_shrieker";
            default -> null;
        };
    }
}

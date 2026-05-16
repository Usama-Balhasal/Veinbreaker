package org.ISoma05.veinBreaker.Utils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Utility class that classifies blocks and items into categories used by VeinBreaker.
 */
public final class BlockUtils {

    private BlockUtils() {}

    // =========================================================================
    //  ORES
    // =========================================================================

    private static final Set<Material> ORE_TYPES = Set.of(
        // Overworld ores
        Material.COAL_ORE,
        Material.IRON_ORE,
        Material.COPPER_ORE,
        Material.GOLD_ORE,
        Material.REDSTONE_ORE,
        Material.LAPIS_ORE,
        Material.DIAMOND_ORE,
        Material.EMERALD_ORE,

        // Deepslate variants
        Material.DEEPSLATE_COAL_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.DEEPSLATE_COPPER_ORE,
        Material.DEEPSLATE_GOLD_ORE,
        Material.DEEPSLATE_REDSTONE_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.DEEPSLATE_DIAMOND_ORE,
        Material.DEEPSLATE_EMERALD_ORE,

        // Nether
        Material.NETHER_GOLD_ORE,
        Material.NETHER_QUARTZ_ORE,
        Material.ANCIENT_DEBRIS
    );

    /** Returns true if the block is any kind of mineable ore. */
    public static boolean isOre(Block block) {
        return ORE_TYPES.contains(block.getType());
    }

    /**
     * Returns true if {@code block} belongs to the same ore family as {@code reference}.
     * Stone and deepslate variants of the same ore are treated as the same family,
     * so veins that cross stone/deepslate layer boundaries are mined correctly.
     */
    public static boolean isSameOre(Block block, Block reference) {
        if (!isOre(block)) return false;
        return oreFamily(block.getType()).equals(oreFamily(reference.getType()));
    }

    private static String oreFamily(Material m) {
        return switch (m) {
            case COAL_ORE,          DEEPSLATE_COAL_ORE      -> "coal";
            case IRON_ORE,          DEEPSLATE_IRON_ORE      -> "iron";
            case COPPER_ORE,        DEEPSLATE_COPPER_ORE    -> "copper";
            case GOLD_ORE,          DEEPSLATE_GOLD_ORE      -> "gold";
            case REDSTONE_ORE,      DEEPSLATE_REDSTONE_ORE  -> "redstone";
            case LAPIS_ORE,         DEEPSLATE_LAPIS_ORE     -> "lapis";
            case DIAMOND_ORE,       DEEPSLATE_DIAMOND_ORE   -> "diamond";
            case EMERALD_ORE,       DEEPSLATE_EMERALD_ORE   -> "emerald";
            case NETHER_GOLD_ORE                            -> "nether_gold";
            case NETHER_QUARTZ_ORE                          -> "quartz";
            case ANCIENT_DEBRIS                             -> "ancient_debris";
            default                                         -> m.name();
        };
    }

    // =========================================================================
    //  LOGS / WOOD
    // =========================================================================

    private static final Set<Material> LOG_TYPES = Set.of(
        Material.OAK_LOG,
        Material.SPRUCE_LOG,
        Material.BIRCH_LOG,
        Material.JUNGLE_LOG,
        Material.ACACIA_LOG,
        Material.DARK_OAK_LOG,
        Material.CHERRY_LOG,
        Material.MANGROVE_LOG,
        Material.BAMBOO_BLOCK,

        // Bark-on-all-sides "wood" blocks (same species as logs)
        Material.OAK_WOOD,
        Material.SPRUCE_WOOD,
        Material.BIRCH_WOOD,
        Material.JUNGLE_WOOD,
        Material.ACACIA_WOOD,
        Material.DARK_OAK_WOOD,
        Material.CHERRY_WOOD,
        Material.MANGROVE_WOOD
    );

    public static boolean isLog(Block block) {
        return LOG_TYPES.contains(block.getType());
    }

    /**
     * True if {@code block} is the same species of log as {@code reference}.
     * Matches log ↔ wood (bark) of the same tree species.
     */
    public static boolean isSameLog(Block block, Block reference) {
        if (!isLog(block)) return false;
        return logSpecies(block.getType()).equals(logSpecies(reference.getType()));
    }

    private static String logSpecies(Material m) {
        return switch (m) {
            case OAK_LOG,      OAK_WOOD      -> "oak";
            case SPRUCE_LOG,   SPRUCE_WOOD   -> "spruce";
            case BIRCH_LOG,    BIRCH_WOOD    -> "birch";
            case JUNGLE_LOG,   JUNGLE_WOOD   -> "jungle";
            case ACACIA_LOG,   ACACIA_WOOD   -> "acacia";
            case DARK_OAK_LOG, DARK_OAK_WOOD -> "dark_oak";
            case CHERRY_LOG,   CHERRY_WOOD   -> "cherry";
            case MANGROVE_LOG, MANGROVE_WOOD -> "mangrove";
            case BAMBOO_BLOCK               -> "bamboo";
            default                         -> m.name();
        };
    }

    // =========================================================================
    //  LEAVES
    // =========================================================================

    private static final Set<Material> LEAF_TYPES = Set.of(
        Material.OAK_LEAVES,
        Material.SPRUCE_LEAVES,
        Material.BIRCH_LEAVES,
        Material.JUNGLE_LEAVES,
        Material.ACACIA_LEAVES,
        Material.DARK_OAK_LEAVES,
        Material.CHERRY_LEAVES,
        Material.MANGROVE_LEAVES,
        Material.AZALEA_LEAVES,
        Material.FLOWERING_AZALEA_LEAVES
    );

    public static boolean isLeaf(Block block) {
        return LEAF_TYPES.contains(block.getType());
    }

    // =========================================================================
    //  CROPS
    // =========================================================================

    private static final Set<Material> CROP_TYPES = Set.of(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS,
        Material.NETHER_WART,
        Material.COCOA
    );

    /**
     * Returns true if the block is a fully-grown crop.
     * Partially grown crops are intentionally skipped.
     */
    public static boolean isCrop(Block block) {
        Material mat = block.getType();
        if (!CROP_TYPES.contains(mat)) return false;

        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return true;
    }

    /**
     * Returns true if {@code block} is the same fully-grown crop type as {@code reference}.
     */
    public static boolean isSameCrop(Block block, Block reference) {
        if (!isCrop(block)) return false;
        return block.getType() == reference.getType();
    }

    // =========================================================================
    //  TOOL TYPE CHECKS
    // =========================================================================

    private static final Set<Material> PICKAXES = Set.of(
        Material.WOODEN_PICKAXE,
        Material.STONE_PICKAXE,
        Material.IRON_PICKAXE,
        Material.GOLDEN_PICKAXE,
        Material.DIAMOND_PICKAXE,
        Material.NETHERITE_PICKAXE
    );

    private static final Set<Material> AXES = Set.of(
        Material.WOODEN_AXE,
        Material.STONE_AXE,
        Material.IRON_AXE,
        Material.GOLDEN_AXE,
        Material.DIAMOND_AXE,
        Material.NETHERITE_AXE
    );

    private static final Set<Material> HOES = Set.of(
        Material.WOODEN_HOE,
        Material.STONE_HOE,
        Material.IRON_HOE,
        Material.GOLDEN_HOE,
        Material.DIAMOND_HOE,
        Material.NETHERITE_HOE
    );

    public static boolean isPickaxe(ItemStack item) {
        return item != null && PICKAXES.contains(item.getType());
    }

    public static boolean isAxe(ItemStack item) {
        return item != null && AXES.contains(item.getType());
    }

    public static boolean isHoe(ItemStack item) {
        return item != null && HOES.contains(item.getType());
    }
}

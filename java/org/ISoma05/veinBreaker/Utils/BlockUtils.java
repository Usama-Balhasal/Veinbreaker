package org.ISoma05.veinBreaker.Utils;

import org.ISoma05.veinBreaker.Config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class that classifies blocks and items into categories used by VeinBreaker.
 * Supports configurable sets loaded from ConfigManager with robust fallback sets,
 * ensuring compatibility across Paper/Spigot 1.21.x - 26.2.
 */
public final class BlockUtils {

    private BlockUtils() {}

    // =========================================================================
    //  DEFAULT FALLBACK ORES
    // =========================================================================

    private static final Set<Material> DEFAULT_ORES = resolveMaterials(
        "COAL_ORE", "IRON_ORE", "COPPER_ORE", "GOLD_ORE",
        "REDSTONE_ORE", "LAPIS_ORE", "DIAMOND_ORE", "EMERALD_ORE",
        "DEEPSLATE_COAL_ORE", "DEEPSLATE_IRON_ORE", "DEEPSLATE_COPPER_ORE", "DEEPSLATE_GOLD_ORE",
        "DEEPSLATE_REDSTONE_ORE", "DEEPSLATE_LAPIS_ORE", "DEEPSLATE_DIAMOND_ORE", "DEEPSLATE_EMERALD_ORE",
        "NETHER_GOLD_ORE", "NETHER_QUARTZ_ORE", "ANCIENT_DEBRIS",
        "RAW_IRON_BLOCK", "RAW_COPPER_BLOCK", "RAW_GOLD_BLOCK"
    );

    // =========================================================================
    //  DEFAULT FALLBACK GEODES
    // =========================================================================

    private static final Set<Material> DEFAULT_GEODES = resolveMaterials(
        "AMETHYST_BLOCK", "BUDDING_AMETHYST", "AMETHYST_CLUSTER",
        "LARGE_AMETHYST_BUD", "MEDIUM_AMETHYST_BUD", "SMALL_AMETHYST_BUD",
        "CALCITE", "SMOOTH_BASALT"
    );

    // =========================================================================
    //  DEFAULT FALLBACK CAVE MATERIALS
    // =========================================================================

    private static final Set<Material> DEFAULT_CAVES = resolveMaterials(
        "DRIPSTONE_BLOCK", "POINTED_DRIPSTONE", "GLOW_LICHEN",
        "SCULK", "SCULK_CATALYST", "SCULK_SENSOR", "SCULK_SHRIEKER", "SCULK_VEIN",
        "TUFF", "POLISHED_TUFF", "CHISELED_TUFF", "TUFF_BRICKS"
    );

    // =========================================================================
    //  DEFAULT FALLBACK LOGS / WOOD
    // =========================================================================

    private static final Set<Material> DEFAULT_LOGS = resolveMaterials(
        "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG",
        "ACACIA_LOG", "DARK_OAK_LOG", "CHERRY_LOG", "MANGROVE_LOG",
        "BAMBOO_BLOCK", "PALE_OAK_LOG",
        "OAK_WOOD", "SPRUCE_WOOD", "BIRCH_WOOD", "JUNGLE_WOOD",
        "ACACIA_WOOD", "DARK_OAK_WOOD", "CHERRY_WOOD", "MANGROVE_WOOD", "PALE_OAK_WOOD"
    );

    // =========================================================================
    //  DEFAULT FALLBACK LEAVES
    // =========================================================================

    private static final Set<Material> DEFAULT_LEAVES = resolveMaterials(
        "OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES", "JUNGLE_LEAVES",
        "ACACIA_LEAVES", "DARK_OAK_LEAVES", "CHERRY_LEAVES", "MANGROVE_LEAVES",
        "AZALEA_LEAVES", "FLOWERING_AZALEA_LEAVES", "PALE_OAK_LEAVES"
    );

    // =========================================================================
    //  DEFAULT FALLBACK CROPS
    // =========================================================================

    private static final Set<Material> DEFAULT_CROPS = resolveMaterials(
        "WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "NETHER_WART", "COCOA"
    );

    // =========================================================================
    //  TOOLS
    // =========================================================================

    private static final Set<Material> PICKAXES = resolveMaterials(
        "WOODEN_PICKAXE", "STONE_PICKAXE", "IRON_PICKAXE",
        "GOLDEN_PICKAXE", "DIAMOND_PICKAXE", "NETHERITE_PICKAXE"
    );

    private static final Set<Material> AXES = resolveMaterials(
        "WOODEN_AXE", "STONE_AXE", "IRON_AXE",
        "GOLDEN_AXE", "DIAMOND_AXE", "NETHERITE_AXE"
    );

    private static final Set<Material> HOES = resolveMaterials(
        "WOODEN_HOE", "STONE_HOE", "IRON_HOE",
        "GOLDEN_HOE", "DIAMOND_HOE", "NETHERITE_HOE"
    );

    // =========================================================================
    //  BLOCK CLASSIFICATION
    // =========================================================================

    public static boolean isOre(Block block, ConfigManager config) {
        if (block == null) return false;
        Set<Material> allowed = config.getAllowedOres();
        if (allowed != null && !allowed.isEmpty()) {
            return allowed.contains(block.getType());
        }
        return DEFAULT_ORES.contains(block.getType());
    }

    public static boolean isGeode(Block block, ConfigManager config) {
        if (block == null) return false;
        Set<Material> allowed = config.getAllowedGeodes();
        if (allowed != null && !allowed.isEmpty()) {
            return allowed.contains(block.getType());
        }
        return DEFAULT_GEODES.contains(block.getType());
    }

    public static boolean isCaveBlock(Block block, ConfigManager config) {
        if (block == null) return false;
        Set<Material> allowed = config.getAllowedCaves();
        if (allowed != null && !allowed.isEmpty()) {
            return allowed.contains(block.getType());
        }
        return DEFAULT_CAVES.contains(block.getType());
    }

    public static boolean isLog(Block block, ConfigManager config) {
        if (block == null) return false;
        Set<Material> allowed = config.getAllowedTrees();
        if (allowed != null && !allowed.isEmpty()) {
            return allowed.contains(block.getType());
        }
        return DEFAULT_LOGS.contains(block.getType());
    }

    public static boolean isLeaf(Block block) {
        return block != null && DEFAULT_LEAVES.contains(block.getType());
    }

    public static boolean isCrop(Block block, ConfigManager config) {
        if (block == null) return false;
        Material mat = block.getType();
        Set<Material> allowed = config.getAllowedCrops();
        boolean matches = (allowed != null && !allowed.isEmpty())
            ? allowed.contains(mat)
            : DEFAULT_CROPS.contains(mat);

        if (!matches) return false;

        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return true;
    }

    // =========================================================================
    //  FAMILY EQUIVALENCE
    // =========================================================================

    public static boolean isSameOre(Block block, Block reference, ConfigManager config) {
        if (!isOre(block, config)) return false;
        return getBlockFamily(block.getType()).equals(getBlockFamily(reference.getType()));
    }

    public static boolean isSameGeode(Block block, Block reference, ConfigManager config) {
        if (!isGeode(block, config)) return false;
        return getBlockFamily(block.getType()).equals(getBlockFamily(reference.getType()));
    }

    public static boolean isSameCaveBlock(Block block, Block reference, ConfigManager config) {
        if (!isCaveBlock(block, config)) return false;
        return getBlockFamily(block.getType()).equals(getBlockFamily(reference.getType()));
    }

    public static boolean isSameLog(Block block, Block reference, ConfigManager config) {
        if (!isLog(block, config)) return false;
        return logSpecies(block.getType()).equals(logSpecies(reference.getType()));
    }

    public static boolean isSameCrop(Block block, Block reference, ConfigManager config) {
        if (!isCrop(block, config)) return false;
        return block.getType() == reference.getType();
    }

    /**
     * Maps a Material to a logical grouping family.
     * Blocks in the same family are grouped together when vein-mining.
     */
    public static String getBlockFamily(Material m) {
        String name = m.name();

        // Ores
        if (name.contains("COAL_ORE")) return "coal";
        if (name.contains("IRON_ORE") || name.equals("RAW_IRON_BLOCK")) return "iron";
        if (name.contains("COPPER_ORE") || name.equals("RAW_COPPER_BLOCK")) return "copper";
        if (name.contains("GOLD_ORE") || name.equals("RAW_GOLD_BLOCK")) {
            return name.contains("NETHER") ? "nether_gold" : "gold";
        }
        if (name.contains("REDSTONE_ORE")) return "redstone";
        if (name.contains("LAPIS_ORE")) return "lapis";
        if (name.contains("DIAMOND_ORE")) return "diamond";
        if (name.contains("EMERALD_ORE")) return "emerald";
        if (name.contains("QUARTZ_ORE")) return "quartz";
        if (name.equals("ANCIENT_DEBRIS")) return "ancient_debris";

        // Geodes & Amethyst
        if (name.contains("AMETHYST")) return "amethyst";
        if (name.equals("CALCITE")) return "calcite";
        if (name.equals("SMOOTH_BASALT")) return "smooth_basalt";

        // Caves
        if (name.contains("DRIPSTONE")) return "dripstone";
        if (name.contains("SCULK")) return "sculk";
        if (name.contains("TUFF")) return "tuff";
        if (name.equals("GLOW_LICHEN")) return "glow_lichen";

        return name.toLowerCase();
    }

    private static String logSpecies(Material m) {
        String name = m.name().toLowerCase();
        if (name.contains("pale_oak")) return "pale_oak";
        if (name.contains("dark_oak")) return "dark_oak";
        if (name.contains("oak")) return "oak";
        if (name.contains("spruce")) return "spruce";
        if (name.contains("birch")) return "birch";
        if (name.contains("jungle")) return "jungle";
        if (name.contains("acacia")) return "acacia";
        if (name.contains("cherry")) return "cherry";
        if (name.contains("mangrove")) return "mangrove";
        if (name.contains("bamboo")) return "bamboo";
        return name;
    }

    // =========================================================================
    //  TOOL TYPE CHECKS
    // =========================================================================

    public static boolean isPickaxe(ItemStack item) {
        return item != null && PICKAXES.contains(item.getType());
    }

    public static boolean isAxe(ItemStack item) {
        return item != null && AXES.contains(item.getType());
    }

    public static boolean isHoe(ItemStack item) {
        return item != null && HOES.contains(item.getType());
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    /**
     * Resolves material names safely, ignoring unrecognized names on older/newer servers.
     */
    private static Set<Material> resolveMaterials(String... names) {
        Set<Material> set = new HashSet<>();
        for (String name : names) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                set.add(m);
            }
        }
        return Set.copyOf(set);
    }
}

package org.ISoma05.veinBreaker.Listeners;

import org.ISoma05.veinBreaker.Config.ConfigManager;
import org.ISoma05.veinBreaker.VeinBreaker;
import org.ISoma05.veinBreaker.Utils.BlockUtils;
import org.ISoma05.veinBreaker.Utils.CooldownManager;
import org.ISoma05.veinBreaker.Utils.CooldownManager.Feature;
import org.ISoma05.veinBreaker.Utils.MessageUtils;
import org.ISoma05.veinBreaker.Utils.XPUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public class VeinBreakerListener implements Listener {

    // The six cardinal neighbours used in BFS scans
    private static final BlockFace[] FACES = {
        BlockFace.NORTH, BlockFace.SOUTH,
        BlockFace.EAST,  BlockFace.WEST,
        BlockFace.UP,    BlockFace.DOWN
    };

    private final VeinBreaker   plugin;
    private final ConfigManager config;
    private final CooldownManager cooldowns;

    public VeinBreakerListener(VeinBreaker plugin) {
        this.plugin    = plugin;
        this.config    = plugin.getConfigManager();
        this.cooldowns = plugin.getCooldownManager();
    }

    // =========================================================================
    //  Main event handler
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Creative mode — let vanilla handle everything
        if (player.getGameMode() == GameMode.CREATIVE) return;

        // Must have a use permission
        if (!hasUsePermission(player)) return;

        // Must have veinbreaker toggled on
        if (!plugin.isVeinBreakerEnabled(player.getUniqueId())) return;

        // Blacklisted world check
        String worldName = player.getWorld().getName();
        if (config.getBlacklistedWorlds().contains(worldName)) return;

        Block     broken = event.getBlock();
        ItemStack tool   = player.getInventory().getItemInMainHand();

        if (BlockUtils.isOre(broken) && config.isOreVeinMiningEnabled()) {
            if (!checkToolForOre(player, tool)) return;
            if (checkCooldown(player, Feature.ORE, config.getOreCooldown())) return;
            handleOre(event, player, broken, tool);
            cooldowns.recordUse(player.getUniqueId(), Feature.ORE);

        } else if (BlockUtils.isLog(broken) && config.isTreeFellingEnabled()) {
            if (!checkToolForTree(player, tool)) return;
            if (checkCooldown(player, Feature.TREE, config.getTreeCooldown())) return;
            handleTree(event, player, broken, tool);
            cooldowns.recordUse(player.getUniqueId(), Feature.TREE);

        } else if (BlockUtils.isCrop(broken) && config.isCropHarvestingEnabled()) {
            if (!checkToolForCrop(player, tool)) return;
            if (checkCooldown(player, Feature.CROP, config.getCropCooldown())) return;
            handleCrop(event, player, broken, tool);
            cooldowns.recordUse(player.getUniqueId(), Feature.CROP);
        }
    }

    // =========================================================================
    //  Permission helpers
    // =========================================================================

    /** Returns true if the player has at least one configured use permission. */
    public boolean hasUsePermission(Player player) {
        for (String perm : config.getUsePermissions()) {
            if (player.hasPermission(perm)) return true;
        }
        return false;
    }

    // =========================================================================
    //  Cooldown helpers
    // =========================================================================

    /**
     * Returns {@code true} (and sends message) if the player is still on cooldown.
     */
    private boolean checkCooldown(Player player, Feature feature, int cooldownSecs) {
        int remaining = cooldowns.getRemainingSeconds(player.getUniqueId(), feature, cooldownSecs);
        if (remaining > 0) {
            player.sendMessage(MessageUtils.color(config.getPrefix()) +
                MessageUtils.colorWithTime(config.getMsgCooldown(), remaining));
            return true;
        }
        return false;
    }

    // =========================================================================
    //  Tool requirement helpers
    // =========================================================================

    private boolean checkToolForOre(Player player, ItemStack tool) {
        if (!config.isOreRequiresPickaxe()) return true;
        return BlockUtils.isPickaxe(tool);
    }

    private boolean checkToolForTree(Player player, ItemStack tool) {
        if (!config.isTreeRequiresAxe()) return true;
        return BlockUtils.isAxe(tool);
    }

    private boolean checkToolForCrop(Player player, ItemStack tool) {
        if (!config.isCropRequiresHoe()) return true;
        return BlockUtils.isHoe(tool);
    }

    // =========================================================================
    //  ORE — Fortune / Silk Touch aware, XP drops
    // =========================================================================

    private void handleOre(BlockBreakEvent event, Player player, Block origin, ItemStack tool) {
        int maxVein = config.getMaxVeinSize();
        List<Block> vein = bfs(origin, maxVein, b -> BlockUtils.isSameOre(b, origin));

        config.debug("Ore vein found: " + vein.size() + " blocks at " + formatLoc(origin.getLocation()));

        if (vein.size() <= 1) return; // Nothing extra — let vanilla break the one block normally

        // We control all drops and XP from here
        event.setDropItems(false);
        event.setExpToDrop(0);

        for (Block block : vein) {
            Location centre = block.getLocation().add(0.5, 0.5, 0.5);

            // Drop items (Fortune / Silk Touch respected automatically)
            for (ItemStack drop : block.getDrops(tool, player)) {
                block.getWorld().dropItemNaturally(centre, drop);
            }

            // Drop XP (Silk Touch suppressed inside XPUtils)
            if (config.isXpDropsEnabled()) {
                XPUtils.dropXp(centre, block.getType(), tool, config);
            }

            // Apply tool durability damage (1 per block, respecting Unbreaking)
            damageTool(player, tool);

            block.setType(Material.AIR);
        }
    }

    // =========================================================================
    //  TREE — logs + connected leaves, fixed BFS for giant trees
    // =========================================================================

    private void handleTree(BlockBreakEvent event, Player player, Block origin, ItemStack tool) {
        int maxTree = config.getMaxTreeSize();
        List<Block> logs = bfs(origin, maxTree, b -> BlockUtils.isSameLog(b, origin));

        config.debug("Tree found: " + logs.size() + " logs at " + formatLoc(origin.getLocation()));

        if (logs.size() <= 1) return;

        event.setDropItems(false);

        // Build a set of log positions for O(1) lookups in the leaf BFS
        Set<Block> logSet = new HashSet<>(logs);

        // Break all logs
        for (Block log : logs) {
            Location centre = log.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : log.getDrops(tool, player)) {
                log.getWorld().dropItemNaturally(centre, drop);
            }
            damageTool(player, tool);
            log.setType(Material.AIR);
        }

        // Collect leaves: single shared BFS seeded by all log positions.
        // This handles large canopies without per-log radius restrictions.
        Set<Block> leaves = collectLeaves(logSet, 10);

        config.debug("Leaves to remove: " + leaves.size());

        for (Block leaf : leaves) {
            if (!BlockUtils.isLeaf(leaf)) continue;
            Location centre = leaf.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : leaf.getDrops(tool, player)) {
                leaf.getWorld().dropItemNaturally(centre, drop);
            }
            leaf.setType(Material.AIR);
        }
    }

    /**
     * Multi-source BFS that collects connected leaf blocks reachable from any
     * block in {@code roots} within {@code maxRadius} blocks of the nearest root.
     * <p>
     * Using a single shared BFS rather than per-log radius searches ensures that
     * large, spread canopies (giant spruce, jungle, dark oak) are fully captured.
     */
    private Set<Block> collectLeaves(Set<Block> roots, int maxRadius) {
        Set<Block>   result  = new HashSet<>();
        Set<Block>   visited = new HashSet<>(roots);
        Queue<Block> queue   = new LinkedList<>(roots);

        while (!queue.isEmpty()) {
            Block current = queue.poll();

            for (BlockFace face : FACES) {
                Block neighbour = current.getRelative(face);
                if (visited.contains(neighbour)) continue;
                visited.add(neighbour);

                if (!BlockUtils.isLeaf(neighbour)) continue;

                // Only add if within maxRadius of at least one original log position
                if (withinRadiusOfAny(neighbour, roots, maxRadius)) {
                    result.add(neighbour);
                    queue.add(neighbour);
                }
            }
        }

        return result;
    }

    /** Returns true if {@code block} is within {@code radius} of any block in {@code set}. */
    private boolean withinRadiusOfAny(Block block, Set<Block> set, int radius) {
        for (Block root : set) {
            if (!block.getWorld().equals(root.getWorld())) continue;
            int dx = Math.abs(block.getX() - root.getX());
            int dy = Math.abs(block.getY() - root.getY());
            int dz = Math.abs(block.getZ() - root.getZ());
            if (dx <= radius && dy <= radius && dz <= radius) return true;
        }
        return false;
    }

    // =========================================================================
    //  CROP — harvest + optional replant
    // =========================================================================

    private void handleCrop(BlockBreakEvent event, Player player, Block origin, ItemStack tool) {
        int maxCrop = config.getMaxCropSize();
        List<Block> crops = bfs(origin, maxCrop, b -> BlockUtils.isSameCrop(b, origin));

        config.debug("Crop patch found: " + crops.size() + " blocks at " + formatLoc(origin.getLocation()));

        if (crops.size() <= 1) return;

        event.setDropItems(false);

        boolean replant = config.isCropReplantEnabled();

        for (Block crop : crops) {
            // Drop items
            for (ItemStack drop : crop.getDrops()) {
                crop.getWorld().dropItemNaturally(crop.getLocation().add(0.5, 0.5, 0.5), drop);
            }

            if (replant) {
                // Reset to age 0 (replanted sapling state) rather than breaking to AIR
                resetCrop(crop);
            } else {
                crop.setType(Material.AIR);
            }
        }
    }

    /**
     * Resets a fully-grown crop to age 0 (freshly planted).
     * Handles all Ageable crops. Non-ageable crops are set to AIR.
     */
    private void resetCrop(Block crop) {
        BlockData data = crop.getBlockData();
        if (data instanceof Ageable ageable) {
            ageable.setAge(0);
            crop.setBlockData(ageable);
        } else {
            crop.setType(Material.AIR);
        }
    }

    // =========================================================================
    //  Generic BFS
    // =========================================================================

    /**
     * Breadth-first search collecting connected blocks that satisfy {@code filter},
     * up to {@code maxBlocks}. Always includes {@code origin}.
     */
    private List<Block> bfs(Block origin, int maxBlocks, Predicate<Block> filter) {
        List<Block>  result  = new ArrayList<>();
        Set<Block>   visited = new HashSet<>();
        Queue<Block> queue   = new LinkedList<>();

        queue.add(origin);
        visited.add(origin);
        result.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            for (BlockFace face : FACES) {
                Block neighbour = current.getRelative(face);
                if (visited.contains(neighbour)) continue;
                visited.add(neighbour);

                if (filter.test(neighbour)) {
                    result.add(neighbour);
                    queue.add(neighbour);
                    if (result.size() >= maxBlocks) break;
                }
            }
        }

        return result;
    }

    // =========================================================================
    //  Tool durability
    // =========================================================================

    /**
     * Applies 1 durability damage to the tool, respecting the Unbreaking enchantment.
     * Does nothing if the tool is unbreakable or has no durability (e.g. bare hand).
     */
    private void damageTool(Player player, ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return;
        if (!(tool.getItemMeta() instanceof Damageable meta)) return;
        if (meta.isUnbreakable()) return;

        // Unbreaking: chance to skip damage = level / (level + 1)
        int unbreaking = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
        if (unbreaking > 0) {
            double skipChance = (double) unbreaking / (unbreaking + 1);
            if (Math.random() < skipChance) return;
        }

        int newDamage = meta.getDamage() + 1;
        int maxDurability = tool.getType().getMaxDurability();

        if (newDamage >= maxDurability) {
            // Tool breaks
            player.playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            tool.setAmount(0);
            return;
        }

        meta.setDamage(newDamage);
        tool.setItemMeta(meta);
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private String formatLoc(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}

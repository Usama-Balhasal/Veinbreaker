package org.ISoma05.veinBreaker.Listeners;

import org.ISoma05.veinBreaker.Animation.AnimationManager;
import org.ISoma05.veinBreaker.Config.ConfigManager;
import org.ISoma05.veinBreaker.Utils.BlockUtils;
import org.ISoma05.veinBreaker.Utils.CooldownManager;
import org.ISoma05.veinBreaker.Utils.CooldownManager.Feature;
import org.ISoma05.veinBreaker.Utils.MessageUtils;
import org.ISoma05.veinBreaker.Utils.XPUtils;
import org.ISoma05.veinBreaker.VeinBreaker;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.*;
import java.util.function.Predicate;

public class VeinBreakerListener implements Listener {

    private static final BlockFace[] FACES = {
        BlockFace.NORTH, BlockFace.SOUTH,
        BlockFace.EAST,  BlockFace.WEST,
        BlockFace.UP,    BlockFace.DOWN
    };

    private static final int[][] PROXIMITY_OFFSETS = buildProximityOffsets();

    private static int[][] buildProximityOffsets() {
        List<int[]> list = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    list.add(new int[]{dx, dy, dz});
                }
            }
        }
        return list.toArray(new int[0][]);
    }

    private final VeinBreaker plugin;
    private final ConfigManager config;
    private final CooldownManager cooldowns;
    private final AnimationManager animationManager;

    public VeinBreakerListener(VeinBreaker plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.cooldowns = plugin.getCooldownManager();
        this.animationManager = plugin.getAnimationManager();
    }

    // =========================================================================
    //  Player Join & Quit persistence handlers
    // =========================================================================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().onPlayerQuit(event.getPlayer().getUniqueId());
    }

    // =========================================================================
    //  Outward Block Placement
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!config.isBlockPlacementEnabled()) return;
        if (!hasUsePermission(player)) return;
        if (!plugin.isVeinBreakerEnabled(player.getUniqueId())) return;
        if (!plugin.isPlacementEnabled(player.getUniqueId())) return;

        if (config.isPlacementRequiresSneak() && !player.isSneaking()) return;

        String worldName = player.getWorld().getName();
        if (config.getBlacklistedWorlds().contains(worldName)) return;

        Block placed = event.getBlockPlaced();
        Material mat = placed.getType();

        // Check if placed block is eligible for outward generation/placement
        boolean isEligible = BlockUtils.isOre(placed, config)
            || BlockUtils.isGeode(placed, config)
            || BlockUtils.isCaveBlock(placed, config)
            || BlockUtils.isLog(placed, config);

        if (!isEligible) return;

        Block against = event.getBlockAgainst();

        if (checkCooldown(player, Feature.PLACEMENT, config.getPlacementCooldown())) return;

        // Calculate available count in inventory
        int maxPlacement = config.getMaxPlacementSize();
        List<Block> targetAirBlocks = scanPlacementTargets(placed, against, maxPlacement);

        if (targetAirBlocks.isEmpty()) return;

        cooldowns.recordUse(player.getUniqueId(), Feature.PLACEMENT);

        boolean useAnim = config.isAnimationEnabled() && plugin.isAnimationEnabled(player.getUniqueId());
        if (useAnim) {
            animationManager.animateOutwardPlacement(player, targetAirBlocks, mat, placed.getBlockData());
        } else {
            // Instant placement
            boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
            for (Block b : targetAirBlocks) {
                if (!isCreative) {
                    if (!consumeItem(player, mat)) break;
                }
                b.setBlockData(placed.getBlockData(), true);
            }
        }
    }

    private List<Block> scanPlacementTargets(Block origin, Block against, int maxBlocks) {
        List<Block> result = new ArrayList<>();
        if (origin == null || against == null) return result;

        BlockFace placedFace = against.getFace(origin);
        if (placedFace == null) return result;

        BlockFace supportFace = placedFace.getOppositeFace();
        Material supportMaterial = against.getType();

        // Only expand on the 2D plane perpendicular to the face being placed on
        List<BlockFace> planeFaces = new ArrayList<>();
        for (BlockFace face : FACES) {
            if (face != placedFace && face != supportFace) {
                planeFaces.add(face);
            }
        }

        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            for (BlockFace face : planeFaces) {
                Block neighbour = current.getRelative(face);
                if (visited.contains(neighbour)) continue;
                visited.add(neighbour);

                if (neighbour.getType().isAir()) {
                    // Must be supported on the support face by the same backing block type
                    Block support = neighbour.getRelative(supportFace);
                    if (support.getType() == supportMaterial) {
                        result.add(neighbour);
                        queue.add(neighbour);
                        if (result.size() >= maxBlocks) break;
                    }
                }
            }
        }
        return result;
    }

    private boolean consumeItem(Player player, Material material) {
        ItemStack[] items = player.getInventory().getContents();
        for (ItemStack item : items) {
            if (item != null && item.getType() == material && item.getAmount() > 0) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    //  Main Block Break Event Handler
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE && !config.isAllowCreative()) {
            config.debug("VeinBreaker skipped: player in creative mode and allow-creative is false.");
            return;
        }
        if (!hasUsePermission(player)) {
            config.debug("VeinBreaker skipped: player " + player.getName() + " lacks use permission.");
            return;
        }
        if (!plugin.isVeinBreakerEnabled(player.getUniqueId())) {
            config.debug("VeinBreaker skipped: player " + player.getName() + " has VeinBreaker toggled off.");
            return;
        }
        boolean sneakReq = config.isSneakToActivate();
        boolean sprintReq = config.isSprintToActivate();

        if (sneakReq && !player.isSneaking() && !player.isSprinting()) {
            config.debug("VeinBreaker skipped: sneak-to-activate is true and player is neither sneaking nor sprinting.");
            return;
        }
        if (sprintReq && !player.isSprinting() && !player.isSneaking()) {
            config.debug("VeinBreaker skipped: sprint-to-activate is true and player is not sprinting.");
            return;
        }

        String worldName = player.getWorld().getName();
        if (config.getBlacklistedWorlds().contains(worldName)) {
            config.debug("VeinBreaker skipped: world '" + worldName + "' is blacklisted.");
            return;
        }

        Block broken = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (BlockUtils.isOre(broken, config) && config.isOreVeinMiningEnabled()) {
            if (!checkToolForOre(player, tool)) {
                config.debug("VeinBreaker ore skipped: tool " + tool.getType() + " is not a valid pickaxe.");
                return;
            }
            if (checkCooldown(player, Feature.ORE, config.getOreCooldown())) return;
            handleOre(event, player, broken, tool);
            cooldowns.recordUse(player.getUniqueId(), Feature.ORE);

        } else if (BlockUtils.isGeode(broken, config) && config.isGeodeMiningEnabled()) {
            if (!checkToolForGeode(player, tool)) {
                config.debug("VeinBreaker geode skipped: tool " + tool.getType() + " is not a valid pickaxe.");
                return;
            }
            if (checkCooldown(player, Feature.GEODE, config.getGeodeCooldown())) return;
            handleGeode(event, player, broken, tool);
            cooldowns.recordUse(player.getUniqueId(), Feature.GEODE);

        } else if (BlockUtils.isCaveBlock(broken, config) && config.isCaveMiningEnabled()) {
            if (!checkToolForCave(player, tool)) {
                config.debug("VeinBreaker cave skipped: tool " + tool.getType() + " is not a valid pickaxe.");
                return;
            }
            if (checkCooldown(player, Feature.CAVE, config.getCaveCooldown())) return;
            handleCave(event, player, broken, tool);
            cooldowns.recordUse(player.getUniqueId(), Feature.CAVE);

        } else if (BlockUtils.isLog(broken, config) && config.isTreeFellingEnabled()) {
            if (!checkToolForTree(player, tool)) {
                config.debug("VeinBreaker tree skipped: tool " + tool.getType() + " is not a valid axe.");
                return;
            }
            if (checkCooldown(player, Feature.TREE, config.getTreeCooldown())) return;
            handleTree(event, player, broken, tool);
            cooldowns.recordUse(player.getUniqueId(), Feature.TREE);

        } else if (BlockUtils.isCrop(broken, config) && config.isCropHarvestingEnabled()) {
            if (!checkToolForCrop(player, tool)) {
                config.debug("VeinBreaker crop skipped: tool " + tool.getType() + " is not a valid hoe.");
                return;
            }
            if (checkCooldown(player, Feature.CROP, config.getCropCooldown())) return;
            handleCrop(event, player, broken, tool);
            cooldowns.recordUse(player.getUniqueId(), Feature.CROP);
        }
    }

    // =========================================================================
    //  Permission & Cooldown helpers
    // =========================================================================

    public boolean hasUsePermission(Player player) {
        for (String perm : config.getUsePermissions()) {
            if (player.hasPermission(perm)) return true;
        }
        return false;
    }

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
    //  Tool requirements
    // =========================================================================

    private boolean checkToolForOre(Player player, ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        if (!config.isOreRequiresPickaxe()) return true;
        return BlockUtils.isPickaxe(tool);
    }

    private boolean checkToolForGeode(Player player, ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        if (!config.isGeodeRequiresPickaxe()) return true;
        return BlockUtils.isPickaxe(tool);
    }

    private boolean checkToolForCave(Player player, ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        if (!config.isCaveRequiresPickaxe()) return true;
        return BlockUtils.isPickaxe(tool);
    }

    private boolean checkToolForTree(Player player, ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        if (!config.isTreeRequiresAxe()) return true;
        return BlockUtils.isAxe(tool);
    }

    private boolean checkToolForCrop(Player player, ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        if (!config.isCropRequiresHoe()) return true;
        return BlockUtils.isHoe(tool);
    }

    // =========================================================================
    //  ORE
    // =========================================================================

    private void handleOre(BlockBreakEvent event, Player player, Block origin, ItemStack tool) {
        int maxVein = config.getMaxVeinSize();

        List<Block> vein = config.isProximityDetectionEnabled()
            ? bfsProximity(origin, maxVein, b -> BlockUtils.isSameOre(b, origin, config))
            : bfs(origin, maxVein, b -> BlockUtils.isSameOre(b, origin, config));

        config.debug("Ore vein found: " + vein.size() + " blocks at " + formatLoc(origin.getLocation()));
        if (vein.size() <= 1) return;

        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        event.setDropItems(false);
        event.setExpToDrop(0);

        if (!isCreative) {
            Location originCenter = origin.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : origin.getDrops(tool, player)) {
                giveOrDrop(player, originCenter, drop);
            }
            if (config.isXpDropsEnabled()) {
                if (config.isXpAutoCollect()) {
                    XPUtils.giveXp(player, origin.getType(), tool, config);
                } else {
                    XPUtils.dropXp(originCenter, origin.getType(), tool, config);
                }
            }
            if (config.isToolDurabilityEnabled()) {
                damageTool(player, tool);
            }
        }

        vein.remove(origin);

        boolean useAnim = config.isAnimationEnabled() && plugin.isAnimationEnabled(player.getUniqueId());
        if (useAnim) {
            animationManager.animateOutwardBreak(player, vein, tool, false, null);
        } else {
            instantBreak(player, vein, tool);
        }
    }

    // =========================================================================
    //  GEODE (Amethyst blocks, budding amethyst, clusters, calcite, basalt)
    // =========================================================================

    private void handleGeode(BlockBreakEvent event, Player player, Block origin, ItemStack tool) {
        int maxGeode = config.getMaxGeodeSize();
        boolean protectBudding = config.isProtectBuddingAmethyst();

        Predicate<Block> filter = b -> {
            if (protectBudding && b.getType() == Material.BUDDING_AMETHYST) return false;
            return BlockUtils.isSameGeode(b, origin, config);
        };

        List<Block> geode = config.isProximityDetectionEnabled()
            ? bfsProximity(origin, maxGeode, filter)
            : bfs(origin, maxGeode, filter);

        config.debug("Geode formation found: " + geode.size() + " blocks at " + formatLoc(origin.getLocation()));
        if (geode.size() <= 1) return;

        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        event.setDropItems(false);
        event.setExpToDrop(0);

        if (!isCreative) {
            Location originCenter = origin.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : origin.getDrops(tool, player)) {
                giveOrDrop(player, originCenter, drop);
            }
            if (config.isXpDropsEnabled()) {
                if (config.isXpAutoCollect()) {
                    XPUtils.giveXp(player, origin.getType(), tool, config);
                } else {
                    XPUtils.dropXp(originCenter, origin.getType(), tool, config);
                }
            }
            if (config.isToolDurabilityEnabled()) {
                damageTool(player, tool);
            }
        }

        geode.remove(origin);

        boolean useAnim = config.isAnimationEnabled() && plugin.isAnimationEnabled(player.getUniqueId());
        if (useAnim) {
            animationManager.animateOutwardBreak(player, geode, tool, false, null);
        } else {
            instantBreak(player, geode, tool);
        }
    }

    // =========================================================================
    //  CAVE FORMATIONS (Dripstone, Sculk, Tuff, Raw ore blocks)
    // =========================================================================

    private void handleCave(BlockBreakEvent event, Player player, Block origin, ItemStack tool) {
        int maxCave = config.getMaxCaveSize();

        List<Block> cave = config.isProximityDetectionEnabled()
            ? bfsProximity(origin, maxCave, b -> BlockUtils.isSameCaveBlock(b, origin, config))
            : bfs(origin, maxCave, b -> BlockUtils.isSameCaveBlock(b, origin, config));

        config.debug("Cave formation found: " + cave.size() + " blocks at " + formatLoc(origin.getLocation()));
        if (cave.size() <= 1) return;

        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        event.setDropItems(false);
        event.setExpToDrop(0);

        if (!isCreative) {
            Location originCenter = origin.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : origin.getDrops(tool, player)) {
                giveOrDrop(player, originCenter, drop);
            }
            if (config.isXpDropsEnabled()) {
                if (config.isXpAutoCollect()) {
                    XPUtils.giveXp(player, origin.getType(), tool, config);
                } else {
                    XPUtils.dropXp(originCenter, origin.getType(), tool, config);
                }
            }
            if (config.isToolDurabilityEnabled()) {
                damageTool(player, tool);
            }
        }

        cave.remove(origin);

        boolean useAnim = config.isAnimationEnabled() && plugin.isAnimationEnabled(player.getUniqueId());
        if (useAnim) {
            animationManager.animateOutwardBreak(player, cave, tool, false, null);
        } else {
            instantBreak(player, cave, tool);
        }
    }

    // =========================================================================
    //  TREE
    // =========================================================================

    private void handleTree(BlockBreakEvent event, Player player, Block origin, ItemStack tool) {
        int maxTree = config.getMaxTreeSize();
        List<Block> logs = bfs(origin, maxTree, b -> BlockUtils.isSameLog(b, origin, config));

        config.debug("Tree found: " + logs.size() + " logs at " + formatLoc(origin.getLocation()));
        if (logs.size() <= 1) return;

        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        event.setDropItems(false);

        if (!isCreative) {
            Location originCenter = origin.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : origin.getDrops(tool, player)) {
                giveOrDrop(player, originCenter, drop);
            }
            if (config.isToolDurabilityEnabled()) {
                damageTool(player, tool);
            }
        }

        logs.remove(origin);

        boolean useAnim = config.isAnimationEnabled() && plugin.isAnimationEnabled(player.getUniqueId());

        if (useAnim) {
            animationManager.animateOutwardBreak(player, logs, tool, false, brokenLogs -> {
                Set<Block> rootsForLeaves = new HashSet<>(brokenLogs);
                rootsForLeaves.add(origin);
                Set<Block> leaves = collectLeaves(rootsForLeaves, 10);
                if (!leaves.isEmpty()) {
                    animationManager.animateOutwardBreak(player, new ArrayList<>(leaves), tool, false, null);
                }
            });
        } else {
            Set<Block> brokenLogs = instantBreak(player, logs, tool);
            Set<Block> rootsForLeaves = new HashSet<>(brokenLogs);
            rootsForLeaves.add(origin);
            Set<Block> leaves = collectLeaves(rootsForLeaves, 10);
            for (Block leaf : leaves) {
                if (!BlockUtils.isLeaf(leaf)) continue;
                if (!isCreative) {
                    Location centre = leaf.getLocation().add(0.5, 0.5, 0.5);
                    for (ItemStack drop : leaf.getDrops(tool, player)) {
                        giveOrDrop(player, centre, drop);
                    }
                }

                if (config.isAnimationParticlesEnabled()) {
                    Location centre = leaf.getLocation().add(0.5, 0.5, 0.5);
                    animationManager.spawnBlockParticles(centre, leaf.getType(), leaf.getBlockData());
                }
                leaf.setType(Material.AIR);
            }
        }
    }

    // =========================================================================
    //  CROP (Harvest & Outward Replanting)
    // =========================================================================

    private void handleCrop(BlockBreakEvent event, Player player, Block origin, ItemStack tool) {
        int maxCrop = config.getMaxCropSize();
        List<Block> crops = bfs(origin, maxCrop, b -> BlockUtils.isSameCrop(b, origin, config));

        config.debug("Crop patch found: " + crops.size() + " blocks at " + formatLoc(origin.getLocation()));

        boolean replant = config.isCropReplantEnabled();
        if (crops.size() <= 1 && !replant) return;

        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        event.setDropItems(false);

        final Material originType = origin.getType();
        if (!isCreative) {
            Location originCenter = origin.getLocation().add(0.5, 0.5, 0.5);
            boolean suppressSeeds = replant && isSeedSuppressedCrop(originType);
            for (ItemStack drop : origin.getDrops()) {
                if (suppressSeeds && isSeedItem(drop.getType())) continue;
                giveOrDrop(player, originCenter, drop);
            }
        }

        if (replant) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                origin.setType(originType);
                BlockData bd = origin.getBlockData();
                if (bd instanceof Ageable ageable) {
                    ageable.setAge(0);
                    origin.setBlockData(ageable);
                }
            }, 1L);
        }

        crops.remove(origin);

        boolean useAnim = config.isAnimationEnabled() && plugin.isAnimationEnabled(player.getUniqueId());

        if (useAnim) {
            animationManager.animateOutwardBreak(player, crops, tool, replant, null);
        } else {
            for (Block crop : crops) {
                if (!isCreative) {
                    boolean suppressSeeds = replant && isSeedSuppressedCrop(crop.getType());
                    for (ItemStack drop : crop.getDrops()) {
                        if (suppressSeeds && isSeedItem(drop.getType())) continue;
                        giveOrDrop(player, crop.getLocation().add(0.5, 0.5, 0.5), drop);
                    }
                }

                if (replant) {
                    resetCrop(crop);
                } else {
                    crop.setType(Material.AIR);
                }
            }
        }
    }

    // =========================================================================
    //  Instant break helper (when animation is toggled off)
    // =========================================================================

    private Set<Block> instantBreak(Player player, List<Block> blocks, ItemStack tool) {
        Set<Block> broken = new HashSet<>();
        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        boolean preventBreak = !isCreative && config.isPreventToolBreak() && config.isToolDurabilityEnabled();
        boolean durabilityEnabled = !isCreative && config.isToolDurabilityEnabled();
        boolean xpAutoCollect = !isCreative && config.isXpAutoCollect();
        boolean xpEnabled = !isCreative && config.isXpDropsEnabled();

        for (Block block : blocks) {
            if (block.getType().isAir()) continue;
            if (preventBreak && wouldBreakTool(tool)) break;

            if (!isCreative) {
                Location centre = block.getLocation().add(0.5, 0.5, 0.5);
                for (ItemStack drop : block.getDrops(tool, player)) {
                    giveOrDrop(player, centre, drop);
                }

                if (xpEnabled) {
                    if (xpAutoCollect) {
                        XPUtils.giveXp(player, block.getType(), tool, config);
                    } else {
                        XPUtils.dropXp(centre, block.getType(), tool, config);
                    }
                }

                if (durabilityEnabled) {
                    damageTool(player, tool);
                }
            }

            if (config.isAnimationParticlesEnabled()) {
                Location centre = block.getLocation().add(0.5, 0.5, 0.5);
                animationManager.spawnBlockParticles(centre, block.getType(), block.getBlockData());
            }

            block.setType(Material.AIR);
            broken.add(block);
        }
        return broken;
    }

    // =========================================================================
    //  BFS Traversal
    // =========================================================================

    private List<Block> bfs(Block origin, int maxBlocks, Predicate<Block> filter) {
        List<Block> result = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

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

    private List<Block> bfsProximity(Block origin, int maxBlocks, Predicate<Block> filter) {
        List<Block> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(origin);
        visited.add(blockKey(origin));
        result.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            int cx = current.getX();
            int cy = current.getY();
            int cz = current.getZ();

            for (int[] offset : PROXIMITY_OFFSETS) {
                Block neighbour = current.getWorld().getBlockAt(
                    cx + offset[0],
                    cy + offset[1],
                    cz + offset[2]
                );
                long key = blockKey(neighbour);
                if (visited.contains(key)) continue;
                visited.add(key);

                if (filter.test(neighbour)) {
                    result.add(neighbour);
                    queue.add(neighbour);
                    if (result.size() >= maxBlocks) break;
                }
            }
        }
        return result;
    }

    private Set<Block> collectLeaves(Set<Block> roots, int maxRadius) {
        Set<Block> result = new HashSet<>();
        Set<Block> visited = new HashSet<>(roots);
        Queue<Block> queue = new LinkedList<>(roots);

        while (!queue.isEmpty()) {
            Block current = queue.poll();

            for (BlockFace face : FACES) {
                Block neighbour = current.getRelative(face);
                if (visited.contains(neighbour)) continue;
                visited.add(neighbour);

                if (!BlockUtils.isLeaf(neighbour)) continue;

                if (withinRadiusOfAny(neighbour, roots, maxRadius)) {
                    result.add(neighbour);
                    queue.add(neighbour);
                }
            }
        }
        return result;
    }

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

    private static long blockKey(Block block) {
        return ((long)(block.getX() & 0x3FFFFFF) << 38)
             | ((long)(block.getY() & 0xFFF)     << 26)
             |  (long)(block.getZ() & 0x3FFFFFF);
    }

    private void giveOrDrop(Player player, Location centre, ItemStack drop) {
        if (config.isDirectToInventory()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
            for (ItemStack overflow : leftover.values()) {
                centre.getWorld().dropItemNaturally(centre, overflow);
            }
        } else {
            centre.getWorld().dropItemNaturally(centre, drop);
        }
    }

    private void resetCrop(Block crop) {
        BlockData data = crop.getBlockData();
        if (data instanceof Ageable ageable) {
            ageable.setAge(0);
            crop.setBlockData(ageable);
        } else {
            crop.setType(Material.AIR);
        }
    }

    private boolean isSeedSuppressedCrop(Material cropType) {
        return cropType == Material.WHEAT || cropType == Material.BEETROOTS;
    }

    private boolean isSeedItem(Material material) {
        return material == Material.WHEAT_SEEDS || material == Material.BEETROOT_SEEDS;
    }

    private boolean wouldBreakTool(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return false;
        if (!(tool.getItemMeta() instanceof Damageable meta)) return false;
        if (meta.isUnbreakable()) return false;

        int remaining = tool.getType().getMaxDurability() - meta.getDamage();
        return remaining <= 1;
    }

    private void damageTool(Player player, ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return;
        if (!(tool.getItemMeta() instanceof Damageable meta)) return;
        if (meta.isUnbreakable()) return;

        int unbreaking = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
        if (unbreaking > 0) {
            double skipChance = (double) unbreaking / (unbreaking + 1);
            if (Math.random() < skipChance) return;
        }

        int newDamage = meta.getDamage() + 1;
        int maxDurability = tool.getType().getMaxDurability();

        if (newDamage >= maxDurability) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            tool.setAmount(0);
            return;
        }

        meta.setDamage(newDamage);
        tool.setItemMeta(meta);
    }

    private String formatLoc(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}

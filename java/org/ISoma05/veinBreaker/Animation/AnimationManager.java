package org.ISoma05.veinBreaker.Animation;

import org.ISoma05.veinBreaker.Config.ConfigManager;
import org.ISoma05.veinBreaker.Utils.MessageUtils;
import org.ISoma05.veinBreaker.Utils.XPUtils;
import org.ISoma05.veinBreaker.VeinBreaker;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Handles progressive outward block animations for both block placement and vein breaking.
 * Blocks are sorted radially from the player or origin and processed sequentially in timed waves,
 * delivering a visually stunning ripple effect accompanied by pitch-escalating acoustic chimes.
 */
public class AnimationManager {

    private final VeinBreaker plugin;
    private final ConfigManager config;
    private final Set<BukkitTask> activeTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AnimationManager(VeinBreaker plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    // =========================================================================
    //  OUTWARD PROGRESSIVE BLOCK BREAKING & REPLANTING
    // =========================================================================

    /**
     * Progressively breaks (and optionally replants) blocks sequentially outward from the player.
     *
     * @param player       The player triggering the action.
     * @param blocks       The list of blocks collected by BFS.
     * @param tool         The tool held by the player.
     * @param isReplant    Whether harvested crops should be replanted to age 0.
     * @param onComplete   Callback run when the entire animation concludes.
     */
    public void animateOutwardBreak(
        Player player,
        List<Block> blocks,
        ItemStack tool,
        boolean isReplant,
        Consumer<Set<Block>> onComplete
    ) {
        if (blocks.isEmpty()) return;

        // Sort blocks outward by distance
        Location centerLoc = "ORIGIN".equalsIgnoreCase(config.getAnimationSortOrigin())
            ? blocks.get(0).getLocation()
            : player.getLocation();

        List<Block> sorted = sortOutward(blocks, centerLoc);

        int delayTicks = config.getAnimationDelayTicks();
        int blocksPerStep = config.getAnimationBlocksPerStep();
        boolean preventBreak = config.isPreventToolBreak() && config.isToolDurabilityEnabled();
        boolean durabilityEnabled = config.isToolDurabilityEnabled();
        boolean xpAutoCollect = config.isXpAutoCollect();
        boolean xpEnabled = config.isXpDropsEnabled();
        boolean particles = config.isAnimationParticlesEnabled();
        boolean sounds = config.isAnimationSoundsEnabled();
        boolean pitchShift = config.isAnimationPitchShift();

        Sound breakSound = resolveSound(config.getSoundBreakStep(), Sound.BLOCK_STONE_BREAK);

        Set<Block> actuallyBroken = new HashSet<>();
        Queue<Block> queue = new LinkedList<>(sorted);

        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        BukkitTask task = new BukkitRunnable() {
            private int stepIndex = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || queue.isEmpty()) {
                    finish();
                    return;
                }

                int processedThisTick = 0;
                while (!queue.isEmpty() && processedThisTick < blocksPerStep) {
                    Block block = queue.poll();
                    if (block.getType().isAir()) continue;
                    processedThisTick++;

                    // Prevent tool break check (survival only)
                    if (!isCreative && preventBreak && wouldBreakTool(tool)) {
                        config.debug("Progressive break stopped: tool would break.");
                        finish();
                        return;
                    }

                    Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
                    Material mat = block.getType();
                    BlockData data = block.getBlockData();

                    // Drops handling
                    if (isReplant) {
                        if (!isCreative) {
                            boolean suppressSeeds = isSeedSuppressedCrop(mat);
                            for (ItemStack drop : block.getDrops()) {
                                if (suppressSeeds && isSeedItem(drop.getType())) continue;
                                giveOrDrop(player, blockCenter, drop);
                            }
                        }
                        resetCrop(block);
                    } else {
                        if (!isCreative) {
                            for (ItemStack drop : block.getDrops(tool, player)) {
                                giveOrDrop(player, blockCenter, drop);
                            }

                            if (xpEnabled) {
                                if (xpAutoCollect) {
                                    XPUtils.giveXp(player, mat, tool, config);
                                } else {
                                    XPUtils.dropXp(blockCenter, mat, tool, config);
                                }
                            }
                        }

                        block.setType(Material.AIR);
                    }

                    actuallyBroken.add(block);

                    // Durability (survival only)
                    if (!isCreative && durabilityEnabled) {
                        damageTool(player, tool);
                    }

                    // Particles
                    if (particles) {
                        spawnBlockParticles(blockCenter, mat, data);
                    }
                }

                // Sound ripple with progressive pitch
                if (sounds && processedThisTick > 0) {
                    float pitch = pitchShift
                        ? Math.min(2.0f, 0.7f + (stepIndex * 0.04f))
                        : 1.0f;
                    player.playSound(player.getLocation(), breakSound, 0.6f, pitch);
                }

                stepIndex++;

                if (queue.isEmpty()) {
                    finish();
                }
            }

            private void finish() {
                cancel();
                activeTasks.remove(this);
                if (onComplete != null) {
                    onComplete.accept(actuallyBroken);
                }
            }
        }.runTaskTimer(plugin, 0L, delayTicks);

        activeTasks.add(task);
    }

    // =========================================================================
    //  OUTWARD PROGRESSIVE BLOCK PLACEMENT
    // =========================================================================

    /**
     * Sequentially places blocks outward from the player or clicked origin in a timed wave.
     *
     * @param player       The player placing blocks.
     * @param targetBlocks Air block locations where new blocks will be placed.
     * @param material     The Material to place.
     * @param blockData    The BlockData (including directional facing) to apply.
     */
    public void animateOutwardPlacement(
        Player player,
        List<Block> targetBlocks,
        Material material,
        BlockData blockData
    ) {
        if (targetBlocks.isEmpty()) return;

        Location centerLoc = "ORIGIN".equalsIgnoreCase(config.getAnimationSortOrigin())
            ? targetBlocks.get(0).getLocation()
            : player.getLocation();

        List<Block> sorted = sortOutward(targetBlocks, centerLoc);

        int delayTicks = config.getAnimationDelayTicks();
        int blocksPerStep = config.getAnimationBlocksPerStep();
        boolean particles = config.isAnimationParticlesEnabled();
        boolean sounds = config.isAnimationSoundsEnabled();
        boolean pitchShift = config.isAnimationPitchShift();
        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;

        Sound placeSound = resolveSound(config.getSoundPlaceStep(), Sound.BLOCK_STONE_PLACE);
        Queue<Block> queue = new LinkedList<>(sorted);

        BukkitTask task = new BukkitRunnable() {
            private int stepIndex = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || queue.isEmpty()) {
                    cancel();
                    activeTasks.remove(this);
                    return;
                }

                int placedThisTick = 0;
                while (!queue.isEmpty() && placedThisTick < blocksPerStep) {
                    Block block = queue.poll();

                    // Must still be air
                    if (!block.getType().isAir()) continue;

                    // Check inventory in survival mode
                    if (!isCreative) {
                        if (!consumeItem(player, material)) {
                            config.debug("Outward placement stopped: player ran out of " + material);
                            cancel();
                            activeTasks.remove(this);
                            return;
                        }
                    }

                    block.setBlockData(blockData, true);
                    placedThisTick++;

                    Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
                    if (particles) {
                        spawnBlockParticles(blockCenter, material, blockData);
                    }
                }

                if (sounds && placedThisTick > 0) {
                    float pitch = pitchShift
                        ? Math.min(2.0f, 0.8f + (stepIndex * 0.05f))
                        : 1.0f;
                    player.playSound(player.getLocation(), placeSound, 0.7f, pitch);
                }

                stepIndex++;

                if (queue.isEmpty()) {
                    cancel();
                    activeTasks.remove(this);
                }
            }
        }.runTaskTimer(plugin, 0L, delayTicks);

        activeTasks.add(task);
    }

    // =========================================================================
    //  SORTING & UTILITY
    // =========================================================================

    /**
     * Sorts a list of blocks radially outward based on distance to the center location.
     */
    public List<Block> sortOutward(List<Block> blocks, Location center) {
        List<Block> copy = new ArrayList<>(blocks);
        copy.sort(Comparator.comparingDouble(b ->
            b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(center)
        ));
        return copy;
    }

    /**
     * Consumes 1 item of the given material from the player's inventory.
     * Returns true if successfully consumed, false if none found.
     */
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

    public void spawnBlockParticles(Location center, Material material, BlockData data) {
        if (center == null || center.getWorld() == null) return;
        org.bukkit.World world = center.getWorld();

        // 1. Play authentic block-break visual effect (guaranteed across all Minecraft clients)
        try {
            if (material != null && material.isBlock()) {
                world.playEffect(center, org.bukkit.Effect.STEP_SOUND, material);
            }
        } catch (Throwable ignored) {}

        // 2. High-visibility directional particle burst with velocity
        try {
            if (data != null) {
                world.spawnParticle(Particle.BLOCK, center, 25, 0.3, 0.3, 0.3, 0.15, data);
            } else if (material != null && material.isBlock()) {
                world.spawnParticle(Particle.BLOCK, center, 25, 0.3, 0.3, 0.3, 0.15, material.createBlockData());
            }
        } catch (Throwable ignored) {}

        // 3. Magical accent sparkle depending on mineral family
        try {
            if (material != null) {
                String name = material.name();
                if (name.contains("AMETHYST")) {
                    world.spawnParticle(Particle.END_ROD, center, 4, 0.25, 0.25, 0.25, 0.05);
                } else if (name.contains("SCULK")) {
                    world.spawnParticle(Particle.SCULK_SOUL, center, 3, 0.2, 0.2, 0.2, 0.02);
                } else if (name.contains("DIAMOND") || name.contains("EMERALD")) {
                    world.spawnParticle(Particle.CRIT, center, 6, 0.25, 0.25, 0.25, 0.1);
                } else if (name.contains("GOLD") || name.contains("COPPER") || name.contains("REDSTONE")) {
                    world.spawnParticle(Particle.ENCHANTED_HIT, center, 5, 0.25, 0.25, 0.25, 0.1);
                }
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("deprecation")
    private Sound resolveSound(String soundName, Sound fallback) {
        if (soundName == null || soundName.isBlank()) return fallback;
        try {
            return Sound.valueOf(soundName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            try {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(soundName.trim().toLowerCase());
                Sound s = org.bukkit.Registry.SOUNDS.get(key);
                if (s != null) return s;
            } catch (Throwable ignored) {}
            return fallback;
        }
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
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            tool.setAmount(0);
            return;
        }

        meta.setDamage(newDamage);
        tool.setItemMeta(meta);
    }

    /**
     * Cancels all currently active animations on disable/reload.
     */
    public void shutdown() {
        for (BukkitTask task : activeTasks) {
            try {
                task.cancel();
            } catch (Exception ignored) {}
        }
        activeTasks.clear();
    }
}

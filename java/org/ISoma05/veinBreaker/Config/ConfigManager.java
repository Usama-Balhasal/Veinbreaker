package org.ISoma05.veinBreaker.Config;

import org.ISoma05.veinBreaker.VeinBreaker;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Wraps the plugin's config.yml and exposes typed, null-safe getters.
 * Call {@link #reload()} whenever the config is reloaded at runtime.
 */
public final class ConfigManager {

    private final VeinBreaker plugin;
    private FileConfiguration config;

    // Cached sets of allowed materials
    private Set<Material> allowedOres = Collections.emptySet();
    private Set<Material> allowedGeodes = Collections.emptySet();
    private Set<Material> allowedCaves = Collections.emptySet();
    private Set<Material> allowedTrees = Collections.emptySet();
    private Set<Material> allowedCrops = Collections.emptySet();

    public ConfigManager(VeinBreaker plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Re-reads config.yml from disk and updates cached configurations. */
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadAllowedBlocks();
    }

    private void loadAllowedBlocks() {
        allowedOres = parseMaterials("allowed-blocks.ores");
        allowedGeodes = parseMaterials("allowed-blocks.geodes");
        allowedCaves = parseMaterials("allowed-blocks.caves");
        allowedTrees = parseMaterials("allowed-blocks.trees");
        allowedCrops = parseMaterials("allowed-blocks.crops");
    }

    private Set<Material> parseMaterials(String path) {
        List<String> list = config.getStringList(path);
        if (list == null || list.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Material> set = new HashSet<>();
        for (String entry : list) {
            if (entry == null || entry.isBlank()) continue;
            Material mat = Material.matchMaterial(entry.trim());
            if (mat != null) {
                set.add(mat);
            } else {
                debug("Material '" + entry + "' under '" + path + "' is not recognized on this server version.");
            }
        }
        return Collections.unmodifiableSet(set);
    }

    // ── General ──────────────────────────────────────────────────────────────

    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }

    public boolean getDefaultToggleState() {
        return config.getBoolean("general.default-toggle-state", false);
    }

    // ── Features ─────────────────────────────────────────────────────────────

    public boolean isOreVeinMiningEnabled() {
        return config.getBoolean("features.ore-vein-mining", true);
    }

    public boolean isGeodeMiningEnabled() {
        return config.getBoolean("features.geode-mining", true);
    }

    public boolean isCaveMiningEnabled() {
        return config.getBoolean("features.cave-mining", true);
    }

    public boolean isProtectBuddingAmethyst() {
        return config.getBoolean("features.protect-budding-amethyst", false);
    }

    public boolean isTreeFellingEnabled() {
        return config.getBoolean("features.tree-felling", true);
    }

    public boolean isCropHarvestingEnabled() {
        return config.getBoolean("features.crop-harvesting", true);
    }

    public boolean isCropReplantEnabled() {
        return config.getBoolean("features.crop-replant", true);
    }

    public boolean isBlockPlacementEnabled() {
        return config.getBoolean("features.block-placement", false);
    }

    public boolean isXpDropsEnabled() {
        return config.getBoolean("features.xp-drops", true);
    }

    // ── Animation ────────────────────────────────────────────────────────────

    public boolean isAnimationEnabled() {
        return config.getBoolean("animation.enabled", true);
    }

    public int getAnimationDelayTicks() {
        return Math.max(1, config.getInt("animation.delay-ticks", 1));
    }

    public int getAnimationBlocksPerStep() {
        return Math.max(1, config.getInt("animation.blocks-per-step", 2));
    }

    public String getAnimationSortOrigin() {
        return config.getString("animation.sort-origin", "PLAYER");
    }

    public boolean isAnimationParticlesEnabled() {
        return config.getBoolean("animation.particles", true);
    }

    public boolean isAnimationSoundsEnabled() {
        return config.getBoolean("animation.sounds", true);
    }

    public boolean isAnimationPitchShift() {
        return config.getBoolean("animation.sound-pitch-shift", true);
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    public List<String> getUsePermissions() {
        List<?> raw = config.getList("permissions.use");
        if (raw != null && !raw.isEmpty()) {
            return raw.stream().map(Object::toString).toList();
        }
        return List.of("veinbreaker.use");
    }

    public List<String> getAdminPermissions() {
        List<?> raw = config.getList("permissions.admin");
        if (raw != null && !raw.isEmpty()) {
            return raw.stream().map(Object::toString).toList();
        }
        return List.of("veinbreaker.admin");
    }

    public void setUsePermissions(List<String> perms) {
        config.set("permissions.use", perms);
        plugin.saveConfig();
    }

    // ── Limits ────────────────────────────────────────────────────────────────

    public int getMaxVeinSize() {
        return config.getInt("limits.max-vein-size", 64);
    }

    public int getMaxGeodeSize() {
        return config.getInt("limits.max-geode-size", 64);
    }

    public int getMaxCaveSize() {
        return config.getInt("limits.max-cave-size", 64);
    }

    public int getMaxTreeSize() {
        return config.getInt("limits.max-tree-size", 400);
    }

    public int getMaxCropSize() {
        return config.getInt("limits.max-crop-size", 128);
    }

    public int getMaxPlacementSize() {
        return config.getInt("limits.max-placement-size", 64);
    }

    // ── Allowed Blocks ────────────────────────────────────────────────────────

    public Set<Material> getAllowedOres() {
        return allowedOres;
    }

    public Set<Material> getAllowedGeodes() {
        return allowedGeodes;
    }

    public Set<Material> getAllowedCaves() {
        return allowedCaves;
    }

    public Set<Material> getAllowedTrees() {
        return allowedTrees;
    }

    public Set<Material> getAllowedCrops() {
        return allowedCrops;
    }

    // ── XP ────────────────────────────────────────────────────────────────────

    public int getXpMin(String family) {
        return config.getInt("xp." + family + ".min", 0);
    }

    public int getXpMax(String family) {
        return config.getInt("xp." + family + ".max", 0);
    }

    // ── Tools ─────────────────────────────────────────────────────────────────

    public boolean isOreRequiresPickaxe() {
        return config.getBoolean("tools.ore-requires-pickaxe", true);
    }

    public boolean isGeodeRequiresPickaxe() {
        return config.getBoolean("tools.geode-requires-pickaxe", true);
    }

    public boolean isCaveRequiresPickaxe() {
        return config.getBoolean("tools.cave-requires-pickaxe", true);
    }

    public boolean isTreeRequiresAxe() {
        return config.getBoolean("tools.tree-requires-axe", true);
    }

    public boolean isCropRequiresHoe() {
        return config.getBoolean("tools.crop-requires-hoe", false);
    }

    public boolean isPlacementRequiresSneak() {
        return config.getBoolean("tools.placement-requires-sneak", true);
    }

    // ── Cooldowns (seconds) ───────────────────────────────────────────────────

    public int getOreCooldown() {
        return config.getInt("cooldowns.ore", 0);
    }

    public int getGeodeCooldown() {
        return config.getInt("cooldowns.geode", 0);
    }

    public int getCaveCooldown() {
        return config.getInt("cooldowns.cave", 0);
    }

    public int getTreeCooldown() {
        return config.getInt("cooldowns.tree", 0);
    }

    public int getCropCooldown() {
        return config.getInt("cooldowns.crop", 0);
    }

    public int getPlacementCooldown() {
        return config.getInt("cooldowns.placement", 0);
    }

    // ── Blacklisted worlds ────────────────────────────────────────────────────

    public List<String> getBlacklistedWorlds() {
        List<?> raw = config.getList("blacklisted-worlds");
        if (raw != null) {
            return raw.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    // ── Behaviour ─────────────────────────────────────────────────────────────

    public boolean isSneakToActivate() {
        return config.getBoolean("behaviour.sneak-to-activate", false);
    }

    public boolean isSprintToActivate() {
        return config.getBoolean("behaviour.sprint-to-activate", false);
    }

    public boolean isAllowCreative() {
        return config.getBoolean("behaviour.allow-creative", true);
    }

    public boolean isDirectToInventory() {
        return config.getBoolean("behaviour.direct-to-inventory", true);
    }

    public boolean isXpAutoCollect() {
        return config.getBoolean("behaviour.xp-auto-collect", true);
    }

    // ── Tool Durability ───────────────────────────────────────────────────────

    public boolean isToolDurabilityEnabled() {
        return config.getBoolean("tool-durability.enabled", true);
    }

    public boolean isPreventToolBreak() {
        return config.getBoolean("tool-durability.prevent-tool-break", true);
    }

    // ── Vein Detection ────────────────────────────────────────────────────────

    public boolean isProximityDetectionEnabled() {
        return config.getBoolean("vein-detection.proximity-detection", true);
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    public String getPrefix() {
        return config.getString("messages.prefix", "&8[&bVeinBreaker&8]&r ");
    }

    public String getMsgEnabled() {
        return config.getString("messages.enabled", "&aVeinBreaker &2enabled&a. Break a vein or place a block to activate!");
    }

    public String getMsgEnabledSneak() {
        return config.getString("messages.enabled-sneak", "&aVeinBreaker &2enabled&a. &7(Hold Shift while mining to activate)");
    }

    public String getMsgDisabled() {
        return config.getString("messages.disabled", "&cVeinBreaker &4disabled&c.");
    }

    public String getMsgPlacementEnabled() {
        return config.getString("messages.placement-enabled", "&aOutward block placement &2enabled&a.");
    }

    public String getMsgPlacementDisabled() {
        return config.getString("messages.placement-disabled", "&cOutward block placement &4disabled&c.");
    }

    public String getMsgAnimEnabled() {
        return config.getString("messages.anim-enabled", "&aOutward animations &2enabled&a.");
    }

    public String getMsgAnimDisabled() {
        return config.getString("messages.anim-disabled", "&cOutward animations &4disabled&c.");
    }

    public String getMsgNoPermission() {
        return config.getString("messages.no-permission", "&cYou don't have permission to use VeinBreaker.");
    }

    public String getMsgReloadSuccess() {
        return config.getString("messages.reload-success", "&aConfiguration reloaded successfully.");
    }

    public String getMsgCooldown() {
        return config.getString("messages.cooldown", "&cPlease wait &e{time}s &cbefore using VeinBreaker again.");
    }

    public String getMsgConsoleOnly() {
        return config.getString("messages.console-only", "&cOnly players can use this command.");
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    public boolean isSoundsEnabled() {
        return config.getBoolean("sounds.enabled", true);
    }

    public String getSoundToggleOn() {
        return config.getString("sounds.toggle-on", "BLOCK_NOTE_BLOCK_PLING");
    }

    public String getSoundToggleOff() {
        return config.getString("sounds.toggle-off", "BLOCK_NOTE_BLOCK_BASS");
    }

    public String getSoundPlaceStep() {
        return config.getString("sounds.place-step", "BLOCK_AMETHYST_BLOCK_STEP");
    }

    public String getSoundBreakStep() {
        return config.getString("sounds.break-step", "BLOCK_AMETHYST_BLOCK_BREAK");
    }

    // ── Debug helper ──────────────────────────────────────────────────────────

    public void debug(String message) {
        if (isDebug()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}

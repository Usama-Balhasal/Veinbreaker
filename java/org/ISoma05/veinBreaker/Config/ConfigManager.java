package org.ISoma05.veinBreaker.Config;

import org.ISoma05.veinBreaker.VeinBreaker;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * Wraps the plugin's config.yml and exposes typed, null-safe getters.
 * Call {@link #reload()} whenever the config is reloaded at runtime.
 */
public final class ConfigManager {

    private final VeinBreaker plugin;
    private FileConfiguration config;

    public ConfigManager(VeinBreaker plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Re-reads config.yml from disk. */
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
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

    public boolean isTreeFellingEnabled() {
        return config.getBoolean("features.tree-felling", true);
    }

    public boolean isCropHarvestingEnabled() {
        return config.getBoolean("features.crop-harvesting", true);
    }

    public boolean isXpDropsEnabled() {
        return config.getBoolean("features.xp-drops", true);
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    /** Returns the list of permission nodes that grant use access. */
    public List<String> getUsePermissions() {
        List<?> raw = config.getList("permissions.use");
        if (raw != null && !raw.isEmpty()) {
            return raw.stream().map(Object::toString).toList();
        }
        return List.of("veinbreaker.use");
    }

    /** Returns the list of permission nodes that grant admin access. */
    public List<String> getAdminPermissions() {
        List<?> raw = config.getList("permissions.admin");
        if (raw != null && !raw.isEmpty()) {
            return raw.stream().map(Object::toString).toList();
        }
        return List.of("veinbreaker.admin");
    }

    /** Saves the use-permission list back to the config file. */
    public void setUsePermissions(List<String> perms) {
        config.set("permissions.use", perms);
        plugin.saveConfig();
    }

    // ── Limits ────────────────────────────────────────────────────────────────

    public int getMaxVeinSize() {
        return config.getInt("limits.max-vein-size", 64);
    }

    public int getMaxTreeSize() {
        return config.getInt("limits.max-tree-size", 400);
    }

    public int getMaxCropSize() {
        return config.getInt("limits.max-crop-size", 128);
    }

    // ── XP ────────────────────────────────────────────────────────────────────

    public int getXpMin(String oreFamily) {
        return config.getInt("xp." + oreFamily + ".min", 0);
    }

    public int getXpMax(String oreFamily) {
        return config.getInt("xp." + oreFamily + ".max", 0);
    }

    // ── Tools ─────────────────────────────────────────────────────────────────

    public boolean isOreRequiresPickaxe() {
        return config.getBoolean("tools.ore-requires-pickaxe", true);
    }

    public boolean isTreeRequiresAxe() {
        return config.getBoolean("tools.tree-requires-axe", true);
    }

    public boolean isCropRequiresHoe() {
        return config.getBoolean("tools.crop-requires-hoe", false);
    }

    // ── Cooldowns (seconds) ───────────────────────────────────────────────────

    public int getOreCooldown() {
        return config.getInt("cooldowns.ore", 0);
    }

    public int getTreeCooldown() {
        return config.getInt("cooldowns.tree", 0);
    }

    public int getCropCooldown() {
        return config.getInt("cooldowns.crop", 0);
    }

    // ── Blacklisted worlds ────────────────────────────────────────────────────

    public List<String> getBlacklistedWorlds() {
        List<?> raw = config.getList("blacklisted-worlds");
        if (raw != null) {
            return raw.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    // ── Crop replant ──────────────────────────────────────────────────────────

    public boolean isCropReplantEnabled() {
        return config.getBoolean("features.crop-replant", true);
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    public String getPrefix() {
        return config.getString("messages.prefix", "&8[&bVeinBreaker&8]&r ");
    }

    public String getMsgEnabled() {
        return config.getString("messages.enabled", "&aVeinBreaker &2enabled&a!");
    }

    public String getMsgDisabled() {
        return config.getString("messages.disabled", "&cVeinBreaker &4disabled&c.");
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

    // ── Debug helper ──────────────────────────────────────────────────────────

    public void debug(String message) {
        if (isDebug()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}

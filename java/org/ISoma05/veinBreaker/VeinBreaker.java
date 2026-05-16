package org.ISoma05.veinBreaker;

import org.ISoma05.veinBreaker.Commands.VeinBreakerCommand;
import org.ISoma05.veinBreaker.Config.ConfigManager;
import org.ISoma05.veinBreaker.Listeners.VeinBreakerListener;
import org.ISoma05.veinBreaker.Utils.CooldownManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class VeinBreaker extends JavaPlugin {

    /** Players who currently have VeinBreaker toggled ON. */
    private final Set<UUID> enabledPlayers = new HashSet<>();

    private ConfigManager    configManager;
    private CooldownManager  cooldownManager;
    private VeinBreakerListener listener;

    @Override
    public void onEnable() {
        // ── Save default config if none exists ────────────────────────────────
        saveDefaultConfig();

        // ── Managers ──────────────────────────────────────────────────────────
        configManager  = new ConfigManager(this);
        cooldownManager = new CooldownManager();

        // ── Listener ──────────────────────────────────────────────────────────
        listener = new VeinBreakerListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        // ── Command ───────────────────────────────────────────────────────────
        VeinBreakerCommand cmd = new VeinBreakerCommand(this, listener);
        if (getCommand("veinbreaker") != null) {
            getCommand("veinbreaker").setExecutor(cmd);
            getCommand("veinbreaker").setTabCompleter(cmd);
        } else {
            getLogger().warning("Command 'veinbreaker' is missing from plugin.yml!");
        }

        // ── Startup log ───────────────────────────────────────────────────────
        getLogger().info("╔══════════════════════════════════╗");
        getLogger().info("║     VeinBreaker  v" + getDescription().getVersion() + "          ║");
        getLogger().info("║     by Usama Balhasal             ║");
        getLogger().info("║     iceforge.world                ║");
        getLogger().info("╚══════════════════════════════════╝");
        getLogger().info("Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        enabledPlayers.clear();
        if (cooldownManager != null) cooldownManager.clear();
        getLogger().info("VeinBreaker disabled. Goodbye!");
    }

    // =========================================================================
    //  Toggle helpers
    // =========================================================================

    public boolean isVeinBreakerEnabled(UUID uuid) {
        return enabledPlayers.contains(uuid);
    }

    /**
     * Toggles VeinBreaker for the given player.
     *
     * @return {@code true} if VeinBreaker is now enabled, {@code false} if disabled.
     */
    public boolean toggleVeinBreaker(UUID uuid) {
        if (enabledPlayers.contains(uuid)) {
            enabledPlayers.remove(uuid);
            return false;
        } else {
            enabledPlayers.add(uuid);
            return true;
        }
    }

    // =========================================================================
    //  Reload
    // =========================================================================

    /** Reloads config and re-initialises all config-dependent managers. */
    public void reloadPlugin() {
        configManager.reload();
        getLogger().info("Configuration reloaded.");
    }

    // =========================================================================
    //  Accessors
    // =========================================================================

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}

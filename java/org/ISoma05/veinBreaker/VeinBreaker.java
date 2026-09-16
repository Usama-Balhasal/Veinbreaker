package org.ISoma05.veinBreaker;

import org.ISoma05.veinBreaker.Animation.AnimationManager;
import org.ISoma05.veinBreaker.Commands.VeinBreakerCommand;
import org.ISoma05.veinBreaker.Config.ConfigManager;
import org.ISoma05.veinBreaker.Data.PlayerDataManager;
import org.ISoma05.veinBreaker.Listeners.VeinBreakerListener;
import org.ISoma05.veinBreaker.Utils.CooldownManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class VeinBreaker extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private AnimationManager animationManager;
    private CooldownManager cooldownManager;
    private VeinBreakerListener listener;

    @Override
    public void onEnable() {
        // ── Save default config if none exists ────────────────────────────────
        saveDefaultConfig();

        // ── Managers ──────────────────────────────────────────────────────────
        configManager = new ConfigManager(this);
        playerDataManager = new PlayerDataManager(this);
        animationManager = new AnimationManager(this);
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
        getLogger().info("║     by Usama Balhasal            ║");
        getLogger().info("║     vlx.world                    ║");
        getLogger().info("╚══════════════════════════════════╝");
        getLogger().info("Plugin enabled successfully! Paper/Spigot 1.21.x - 26.2 ready.");
    }

    @Override
    public void onDisable() {
        if (animationManager != null) {
            animationManager.shutdown();
        }
        if (playerDataManager != null) {
            playerDataManager.shutdown();
        }
        if (cooldownManager != null) {
            cooldownManager.clear();
        }
        getLogger().info("VeinBreaker disabled. State saved safely!");
    }

    // =========================================================================
    // Toggle helpers (backed by PlayerDataManager for persistence)
    // =========================================================================

    public boolean isVeinBreakerEnabled(UUID uuid) {
        return playerDataManager.isVeinBreakerEnabled(uuid);
    }

    public boolean toggleVeinBreaker(UUID uuid) {
        return playerDataManager.toggleVeinBreaker(uuid);
    }

    public boolean isPlacementEnabled(UUID uuid) {
        return playerDataManager.isPlacementEnabled(uuid);
    }

    public boolean togglePlacement(UUID uuid) {
        return playerDataManager.togglePlacement(uuid);
    }

    public boolean isAnimationEnabled(UUID uuid) {
        return playerDataManager.isAnimationEnabled(uuid);
    }

    public boolean toggleAnimation(UUID uuid) {
        return playerDataManager.toggleAnimation(uuid);
    }

    // =========================================================================
    // Reload
    // =========================================================================

    /** Reloads config and re-initialises all config-dependent managers. */
    public void reloadPlugin() {
        configManager.reload();
        playerDataManager.loadData();
        getLogger().info("Configuration and persistent data reloaded.");
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}

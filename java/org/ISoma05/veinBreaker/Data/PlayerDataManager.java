package org.ISoma05.veinBreaker.Data;

import org.ISoma05.veinBreaker.VeinBreaker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages persistent player preferences (toggle states, placement mode, animations).
 * Uses in-memory caching for zero-latency gameplay checks, backed by an asynchronous
 * YAML storage file (playerdata.yml) that persists across disconnects, reconnects,
 * server reloads, and restarts.
 */
public class PlayerDataManager {

    public static class PlayerPreferences {
        private boolean veinBreakerEnabled;
        private boolean placementEnabled;
        private boolean animationEnabled;

        public PlayerPreferences(boolean veinBreakerEnabled, boolean placementEnabled, boolean animationEnabled) {
            this.veinBreakerEnabled = veinBreakerEnabled;
            this.placementEnabled = placementEnabled;
            this.animationEnabled = animationEnabled;
        }

        public boolean isVeinBreakerEnabled() {
            return veinBreakerEnabled;
        }

        public void setVeinBreakerEnabled(boolean veinBreakerEnabled) {
            this.veinBreakerEnabled = veinBreakerEnabled;
        }

        public boolean isPlacementEnabled() {
            return placementEnabled;
        }

        public void setPlacementEnabled(boolean placementEnabled) {
            this.placementEnabled = placementEnabled;
        }

        public boolean isAnimationEnabled() {
            return animationEnabled;
        }

        public void setAnimationEnabled(boolean animationEnabled) {
            this.animationEnabled = animationEnabled;
        }
    }

    private final VeinBreaker plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, PlayerPreferences> cache = new ConcurrentHashMap<>();
    private volatile boolean isDirty = false;

    public PlayerDataManager(VeinBreaker plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadData();
    }

    /**
     * Loads all saved player preferences from playerdata.yml into the in-memory cache.
     */
    public synchronized void loadData() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create playerdata.yml!", e);
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        cache.clear();

        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String key : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    boolean enabled = playersSection.getBoolean(key + ".enabled", plugin.getConfigManager().getDefaultToggleState());
                    boolean placement = playersSection.getBoolean(key + ".placement", false);
                    boolean animation = playersSection.getBoolean(key + ".animation", true);
                    cache.put(uuid, new PlayerPreferences(enabled, placement, animation));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Skipping invalid UUID in playerdata.yml: " + key);
                }
            }
        }
    }

    /**
     * Synchronously saves all cached player data to disk.
     */
    public synchronized void saveSync() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }

        for (Map.Entry<UUID, PlayerPreferences> entry : cache.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerPreferences prefs = entry.getValue();
            dataConfig.set(path + ".enabled", prefs.isVeinBreakerEnabled());
            dataConfig.set(path + ".placement", prefs.isPlacementEnabled());
            dataConfig.set(path + ".animation", prefs.isAnimationEnabled());
        }

        try {
            dataConfig.save(dataFile);
            isDirty = false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save playerdata.yml!", e);
        }
    }

    /**
     * Asynchronously saves player data to disk to avoid blocking the main server thread.
     */
    public void saveAsync() {
        isDirty = true;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isDirty) {
                saveSync();
            }
        });
    }

    /**
     * Reads player preferences from dataConfig or creates new defaults.
     * Does NOT touch the cache map to ensure no recursive updates occur.
     */
    public PlayerPreferences loadFromConfig(UUID uuid) {
        String path = "players." + uuid.toString();
        boolean defToggle = plugin.getConfigManager().getDefaultToggleState();
        boolean enabled = dataConfig != null && dataConfig.getBoolean(path + ".enabled", defToggle);
        boolean placement = dataConfig != null && dataConfig.getBoolean(path + ".placement", false);
        boolean animation = dataConfig == null || dataConfig.getBoolean(path + ".animation", true);
        return new PlayerPreferences(enabled, placement, animation);
    }

    /**
     * Called when a player joins the server. Loads existing preference into cache.
     */
    public PlayerPreferences loadPlayer(UUID uuid) {
        PlayerPreferences prefs = cache.get(uuid);
        if (prefs == null) {
            prefs = loadFromConfig(uuid);
            cache.put(uuid, prefs);
        }
        return prefs;
    }

    /**
     * Called when a player quits the server. Persists their state.
     */
    public void onPlayerQuit(UUID uuid) {
        if (cache.containsKey(uuid)) {
            saveAsync();
        }
    }

    /**
     * Retrieves the player's preferences, initializing with defaults if missing.
     */
    public PlayerPreferences getPreferences(UUID uuid) {
        PlayerPreferences prefs = cache.get(uuid);
        if (prefs != null) {
            return prefs;
        }
        return cache.computeIfAbsent(uuid, this::loadFromConfig);
    }

    public boolean isVeinBreakerEnabled(UUID uuid) {
        return getPreferences(uuid).isVeinBreakerEnabled();
    }

    public void setVeinBreakerEnabled(UUID uuid, boolean enabled) {
        getPreferences(uuid).setVeinBreakerEnabled(enabled);
        saveAsync();
    }

    public boolean toggleVeinBreaker(UUID uuid) {
        PlayerPreferences prefs = getPreferences(uuid);
        boolean newState = !prefs.isVeinBreakerEnabled();
        prefs.setVeinBreakerEnabled(newState);
        saveAsync();
        return newState;
    }

    public boolean isPlacementEnabled(UUID uuid) {
        return getPreferences(uuid).isPlacementEnabled();
    }

    public boolean togglePlacement(UUID uuid) {
        PlayerPreferences prefs = getPreferences(uuid);
        boolean newState = !prefs.isPlacementEnabled();
        prefs.setPlacementEnabled(newState);
        saveAsync();
        return newState;
    }

    public boolean isAnimationEnabled(UUID uuid) {
        return getPreferences(uuid).isAnimationEnabled();
    }

    public boolean toggleAnimation(UUID uuid) {
        PlayerPreferences prefs = getPreferences(uuid);
        boolean newState = !prefs.isAnimationEnabled();
        prefs.setAnimationEnabled(newState);
        saveAsync();
        return newState;
    }

    /**
     * Shuts down manager and forces immediate save.
     */
    public void shutdown() {
        saveSync();
        cache.clear();
    }
}

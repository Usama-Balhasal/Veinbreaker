package org.ISoma05.veinBreaker.Utils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages per-player, per-feature cooldowns.
 * All times are in milliseconds internally; the API uses seconds.
 */
public final class CooldownManager {

    public enum Feature {
        ORE, GEODE, CAVE, TREE, CROP, PLACEMENT
    }

    /** feature → (player UUID → last-use timestamp) */
    private final Map<Feature, Map<UUID, Long>> cooldowns = new EnumMap<>(Feature.class);

    public CooldownManager() {
        for (Feature f : Feature.values()) {
            cooldowns.put(f, new HashMap<>());
        }
    }

    /**
     * Returns the number of seconds remaining on the player's cooldown,
     * or {@code 0} if the player is not on cooldown.
     *
     * @param uuid         Player UUID.
     * @param feature      Which feature to check.
     * @param cooldownSecs Configured cooldown duration in seconds.
     */
    public int getRemainingSeconds(UUID uuid, Feature feature, int cooldownSecs) {
        if (cooldownSecs <= 0) return 0;

        Long lastUse = cooldowns.get(feature).get(uuid);
        if (lastUse == null) return 0;

        long elapsed = System.currentTimeMillis() - lastUse;
        long cooldownMs = (long) cooldownSecs * 1000;
        if (elapsed >= cooldownMs) return 0;

        return (int) Math.ceil((cooldownMs - elapsed) / 1000.0);
    }

    /**
     * Records the current time as the last-use timestamp for this player and feature.
     *
     * @param uuid    Player UUID.
     * @param feature Which feature was used.
     */
    public void recordUse(UUID uuid, Feature feature) {
        cooldowns.get(feature).put(uuid, System.currentTimeMillis());
    }

    /** Clears all stored cooldown data (called on plugin disable). */
    public void clear() {
        cooldowns.values().forEach(Map::clear);
    }
}

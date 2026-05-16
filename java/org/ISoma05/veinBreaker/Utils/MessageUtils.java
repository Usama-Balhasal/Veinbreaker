package org.ISoma05.veinBreaker.Utils;

import org.bukkit.ChatColor;

/**
 * Utility class for building and colour-translating messages.
 */
public final class MessageUtils {

    private MessageUtils() {}

    /**
     * Translates {@code &} colour codes to Bukkit {@link ChatColor} codes.
     *
     * @param message Raw message with {@code &} colour codes.
     * @return Colour-formatted string ready to send to a player.
     */
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Applies colour translation to a message that contains a
     * {@code {time}} placeholder, replacing it with the given value.
     */
    public static String colorWithTime(String message, int seconds) {
        return color(message.replace("{time}", String.valueOf(seconds)));
    }
}

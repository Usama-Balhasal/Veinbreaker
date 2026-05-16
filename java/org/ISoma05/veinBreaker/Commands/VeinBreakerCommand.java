package org.ISoma05.veinBreaker.Commands;

import org.ISoma05.veinBreaker.Config.ConfigManager;
import org.ISoma05.veinBreaker.Listeners.VeinBreakerListener;
import org.ISoma05.veinBreaker.Utils.MessageUtils;
import org.ISoma05.veinBreaker.VeinBreaker;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles all /veinbreaker (alias /vb) sub-commands.
 *
 * <pre>
 *   /vb [toggle]              — Toggle vein-mining on/off
 *   /vb help                  — Show this help page
 *   /vb status                — Show current toggle state
 *   /vb reload                — Reload configuration (admin)
 *   /vb permission add <node> — Add a use-permission node (admin)
 *   /vb permission remove <node> — Remove a use-permission node (admin)
 * </pre>
 */
public class VeinBreakerCommand implements CommandExecutor, TabCompleter {

    private final VeinBreaker plugin;
    private final ConfigManager config;
    private final VeinBreakerListener listener;

    public VeinBreakerCommand(VeinBreaker plugin, VeinBreakerListener listener) {
        this.plugin   = plugin;
        this.config   = plugin.getConfigManager();
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // No args or first arg is "toggle" → toggle command (players only)
        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            return cmdToggle(sender);
        }

        switch (args[0].toLowerCase()) {
            case "help"       -> { return cmdHelp(sender); }
            case "status"     -> { return cmdStatus(sender); }
            case "reload"     -> { return cmdReload(sender); }
            case "permission" -> { return cmdPermission(sender, args); }
            default -> {
                sender.sendMessage(prefix() + MessageUtils.color(
                    "&cUnknown sub-command. Use &e/vb help &cfor a list."));
                return true;
            }
        }
    }

    // =========================================================================
    //  Sub-commands
    // =========================================================================

    /** Toggle VeinBreaker on/off for the calling player. */
    private boolean cmdToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix() + MessageUtils.color(config.getMsgConsoleOnly()));
            return true;
        }

        if (!listener.hasUsePermission(player)) {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgNoPermission()));
            return true;
        }

        boolean nowEnabled = plugin.toggleVeinBreaker(player.getUniqueId());

        if (nowEnabled) {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgEnabled()));
            playSound(player, config.getSoundToggleOn());
        } else {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgDisabled()));
            playSound(player, config.getSoundToggleOff());
        }
        return true;
    }

    /** Display the help page. */
    private boolean cmdHelp(CommandSender sender) {
        String line = MessageUtils.color("&8&m------------------------------------");
        sender.sendMessage(line);
        sender.sendMessage(MessageUtils.color("  &b&lVeinBreaker &8| &7Help"));
        sender.sendMessage(line);
        sender.sendMessage(MessageUtils.color("  &e/vb &8- &7Toggle vein-mining on/off"));
        sender.sendMessage(MessageUtils.color("  &e/vb toggle &8- &7Alias for above"));
        sender.sendMessage(MessageUtils.color("  &e/vb status &8- &7View current state"));
        sender.sendMessage(MessageUtils.color("  &e/vb help &8- &7Show this help page"));
        sender.sendMessage(MessageUtils.color("  &8&oAdmin Commands:"));
        sender.sendMessage(MessageUtils.color("  &e/vb reload &8- &7Reload configuration"));
        sender.sendMessage(MessageUtils.color("  &e/vb permission add <node> &8- &7Add a use-permission"));
        sender.sendMessage(MessageUtils.color("  &e/vb permission remove <node> &8- &7Remove a use-permission"));
        sender.sendMessage(line);
        sender.sendMessage(MessageUtils.color("  &7Permissions: &eveinbreaker.use &7| &eveinbreaker.admin"));
        sender.sendMessage(MessageUtils.color("  &7Website: &bhttps://www.iceforge.world/"));
        sender.sendMessage(line);
        return true;
    }

    /** Show the player's current toggle state and active settings. */
    private boolean cmdStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix() + MessageUtils.color("&cOnly players can check status."));
            return true;
        }

        boolean enabled = plugin.isVeinBreakerEnabled(player.getUniqueId());
        String state    = enabled
            ? MessageUtils.color("&a✔ Enabled")
            : MessageUtils.color("&c✘ Disabled");

        String line = MessageUtils.color("&8&m------------------------------------");
        player.sendMessage(line);
        player.sendMessage(MessageUtils.color("  &b&lVeinBreaker &8| &7Status"));
        player.sendMessage(line);
        player.sendMessage(MessageUtils.color("  &7State: ") + state);
        player.sendMessage(MessageUtils.color("  &7Ore mining: &e" + config.isOreVeinMiningEnabled()));
        player.sendMessage(MessageUtils.color("  &7Tree felling: &e" + config.isTreeFellingEnabled()));
        player.sendMessage(MessageUtils.color("  &7Crop harvesting: &e" + config.isCropHarvestingEnabled()));
        player.sendMessage(MessageUtils.color("  &7Crop replant: &e" + config.isCropReplantEnabled()));
        player.sendMessage(MessageUtils.color("  &7XP drops: &e" + config.isXpDropsEnabled()));
        player.sendMessage(MessageUtils.color("  &7Max vein size: &e" + config.getMaxVeinSize()));
        player.sendMessage(MessageUtils.color("  &7Max tree size: &e" + config.getMaxTreeSize()));
        player.sendMessage(line);
        return true;
    }

    /** Reload configuration — requires veinbreaker.admin or veinbreaker.reload. */
    private boolean cmdReload(CommandSender sender) {
        if (!hasAdminPermission(sender) && !sender.hasPermission("veinbreaker.reload")) {
            sender.sendMessage(prefix() + MessageUtils.color(config.getMsgNoPermission()));
            return true;
        }

        plugin.reloadPlugin();
        sender.sendMessage(prefix() + MessageUtils.color(config.getMsgReloadSuccess()));
        return true;
    }

    /** Add or remove a permission node from the use-permission list. */
    private boolean cmdPermission(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(prefix() + MessageUtils.color(config.getMsgNoPermission()));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(prefix() + MessageUtils.color(
                "&cUsage: &e/vb permission <add|remove> <node>"));
            return true;
        }

        String action = args[1].toLowerCase();
        String node   = args[2];

        List<String> perms = new ArrayList<>(config.getUsePermissions());

        switch (action) {
            case "add" -> {
                if (perms.contains(node)) {
                    sender.sendMessage(prefix() + MessageUtils.color(
                        "&ePermission &b" + node + " &eis already in the list."));
                } else {
                    perms.add(node);
                    config.setUsePermissions(perms);
                    sender.sendMessage(prefix() + MessageUtils.color(
                        "&aAdded permission &b" + node + " &ato the use list."));
                }
            }
            case "remove" -> {
                if (!perms.contains(node)) {
                    sender.sendMessage(prefix() + MessageUtils.color(
                        "&cPermission &b" + node + " &cwas not found in the list."));
                } else {
                    perms.remove(node);
                    if (perms.isEmpty()) {
                        sender.sendMessage(prefix() + MessageUtils.color(
                            "&cCannot remove the last permission — at least one must remain."));
                        return true;
                    }
                    config.setUsePermissions(perms);
                    sender.sendMessage(prefix() + MessageUtils.color(
                        "&cRemoved permission &b" + node + " &cfrom the use list."));
                }
            }
            default -> sender.sendMessage(prefix() + MessageUtils.color(
                "&cUnknown action. Use &eadd &cor &eremove&c."));
        }
        return true;
    }

    // =========================================================================
    //  Tab completion
    // =========================================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("toggle", "help", "status"));
            if (hasAdminPermission(sender)) {
                subs.add("reload");
                subs.add("permission");
            }
            String partial = args[0].toLowerCase();
            for (String s : subs) {
                if (s.startsWith(partial)) completions.add(s);
            }

        } else if (args.length == 2 && args[0].equalsIgnoreCase("permission") && hasAdminPermission(sender)) {
            for (String a : List.of("add", "remove")) {
                if (a.startsWith(args[1].toLowerCase())) completions.add(a);
            }

        } else if (args.length == 3 && args[0].equalsIgnoreCase("permission")
                && args[1].equalsIgnoreCase("remove") && hasAdminPermission(sender)) {
            // Suggest currently registered permission nodes for removal
            String partial = args[2].toLowerCase();
            for (String p : config.getUsePermissions()) {
                if (p.toLowerCase().startsWith(partial)) completions.add(p);
            }
        }

        return completions;
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private String prefix() {
        return MessageUtils.color(config.getPrefix());
    }

    private boolean hasAdminPermission(CommandSender sender) {
        for (String perm : config.getAdminPermissions()) {
            if (sender.hasPermission(perm)) return true;
        }
        return false;
    }

    /** Plays a named sound on the player. Silently ignores invalid sound names. */
    private void playSound(Player player, String soundName) {
        if (!config.isSoundsEnabled()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
            // Invalid sound name in config — skip gracefully
        }
    }
}

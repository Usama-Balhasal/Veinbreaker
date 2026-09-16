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
 *   /vb [toggle]              — Toggle main vein-mining state
 *   /vb toggle <place|anim>   — Toggle outward placement or animation
 *   /vb place                 — Shortcut to toggle outward placement
 *   /vb anim                  — Shortcut to toggle outward animation
 *   /vb help                  — Show this help page
 *   /vb status                — Show current toggle state and settings
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
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // No args or /vb toggle
        if (args.length == 0) {
            return cmdToggleMain(sender);
        }

        switch (args[0].toLowerCase()) {
            case "toggle" -> {
                return cmdToggle(sender, args);
            }
            case "place" -> {
                return cmdTogglePlace(sender);
            }
            case "anim", "animation" -> {
                return cmdToggleAnim(sender);
            }
            case "help" -> {
                return cmdHelp(sender);
            }
            case "status" -> {
                return cmdStatus(sender);
            }
            case "reload" -> {
                return cmdReload(sender);
            }
            case "permission" -> {
                return cmdPermission(sender, args);
            }
            default -> {
                sender.sendMessage(prefix() + MessageUtils.color(
                        "&cUnknown sub-command. Use &e/vb help &cfor a list."));
                return true;
            }
        }
    }

    // =========================================================================
    // Sub-commands
    // =========================================================================

    private boolean cmdToggle(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String target = args[1].toLowerCase();
            if (target.startsWith("place")) {
                return cmdTogglePlace(sender);
            } else if (target.startsWith("anim")) {
                return cmdToggleAnim(sender);
            }
        }
        return cmdToggleMain(sender);
    }

    private boolean cmdToggleMain(CommandSender sender) {
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
            String msg = config.isSneakToActivate() ? config.getMsgEnabledSneak() : config.getMsgEnabled();
            player.sendMessage(prefix() + MessageUtils.color(msg));
            playSound(player, config.getSoundToggleOn());
        } else {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgDisabled()));
            playSound(player, config.getSoundToggleOff());
        }
        return true;
    }

    private boolean cmdTogglePlace(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix() + MessageUtils.color(config.getMsgConsoleOnly()));
            return true;
        }

        if (!listener.hasUsePermission(player)) {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgNoPermission()));
            return true;
        }

        boolean nowEnabled = plugin.togglePlacement(player.getUniqueId());

        if (nowEnabled) {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgPlacementEnabled()));
            playSound(player, config.getSoundToggleOn());
        } else {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgPlacementDisabled()));
            playSound(player, config.getSoundToggleOff());
        }
        return true;
    }

    private boolean cmdToggleAnim(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix() + MessageUtils.color(config.getMsgConsoleOnly()));
            return true;
        }

        if (!listener.hasUsePermission(player)) {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgNoPermission()));
            return true;
        }

        boolean nowEnabled = plugin.toggleAnimation(player.getUniqueId());

        if (nowEnabled) {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgAnimEnabled()));
            playSound(player, config.getSoundToggleOn());
        } else {
            player.sendMessage(prefix() + MessageUtils.color(config.getMsgAnimDisabled()));
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
        sender.sendMessage(MessageUtils.color("  &e/vb place &8- &7Toggle outward block placement"));
        sender.sendMessage(MessageUtils.color("  &e/vb anim &8- &7Toggle outward animations"));
        sender.sendMessage(MessageUtils.color("  &e/vb status &8- &7View current persistent state"));
        sender.sendMessage(MessageUtils.color("  &e/vb help &8- &7Show this help page"));
        sender.sendMessage(MessageUtils.color("  &8&oAdmin Commands:"));
        sender.sendMessage(MessageUtils.color("  &e/vb reload &8- &7Reload configuration & data"));
        sender.sendMessage(MessageUtils.color("  &e/vb permission add <node> &8- &7Add a use-permission"));
        sender.sendMessage(MessageUtils.color("  &e/vb permission remove <node> &8- &7Remove a use-permission"));
        sender.sendMessage(line);
        sender.sendMessage(MessageUtils.color("  &7Permissions: &eveinbreaker.use &7| &eveinbreaker.admin"));
        sender.sendMessage(MessageUtils.color("  &7Website: &bhttps://www.vlx.world/"));
        sender.sendMessage(line);
        return true;
    }

    /** Show the player's current persistent toggle states and active settings. */
    private boolean cmdStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix() + MessageUtils.color("&cOnly players can check status."));
            return true;
        }

        boolean vbEnabled = plugin.isVeinBreakerEnabled(player.getUniqueId());
        boolean plEnabled = plugin.isPlacementEnabled(player.getUniqueId());
        boolean anEnabled = plugin.isAnimationEnabled(player.getUniqueId());

        String vbState = vbEnabled ? MessageUtils.color("&a✔ Enabled") : MessageUtils.color("&c✘ Disabled");
        String plState = plEnabled ? MessageUtils.color("&a✔ Enabled") : MessageUtils.color("&c✘ Disabled");
        String anState = anEnabled ? MessageUtils.color("&a✔ Enabled") : MessageUtils.color("&c✘ Disabled");
        String sneakState = (config.isSneakToActivate() || config.isSprintToActivate())
                ? MessageUtils.color("&eRequired (Hold Shift or Sprint)")
                : MessageUtils.color("&aNot required (Active on break)");
        String creativeState = config.isAllowCreative() ? MessageUtils.color("&a✔ Enabled")
                : MessageUtils.color("&c✘ Disabled");

        String line = MessageUtils.color("&8&m------------------------------------");
        player.sendMessage(line);
        player.sendMessage(MessageUtils.color("  &b&lVeinBreaker &8| &7Status &8(Saved & Persisted)"));
        player.sendMessage(line);
        player.sendMessage(MessageUtils.color("  &7VeinBreaker: ") + vbState);
        player.sendMessage(MessageUtils.color("  &7Outward Placement: ") + plState);
        player.sendMessage(MessageUtils.color("  &7Outward Animation: ") + anState);
        player.sendMessage(MessageUtils.color("  &7Sneak to mine: ") + sneakState);
        player.sendMessage(MessageUtils.color("  &7Creative mining: ") + creativeState);
        player.sendMessage(MessageUtils.color("  &7Ore mining: &e" + config.isOreVeinMiningEnabled()));
        player.sendMessage(MessageUtils.color("  &7Geode mining: &e" + config.isGeodeMiningEnabled()));
        player.sendMessage(MessageUtils.color("  &7Cave mining: &e" + config.isCaveMiningEnabled()));
        player.sendMessage(MessageUtils.color("  &7Tree felling: &e" + config.isTreeFellingEnabled()));
        player.sendMessage(MessageUtils.color("  &7Crop harvesting: &e" + config.isCropHarvestingEnabled()));
        player.sendMessage(MessageUtils.color("  &7Crop replant: &e" + config.isCropReplantEnabled()));
        player.sendMessage(MessageUtils.color("  &7Max vein size: &e" + config.getMaxVeinSize()));
        player.sendMessage(MessageUtils.color("  &7Max geode size: &e" + config.getMaxGeodeSize()));
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
        String node = args[2];

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
    // Tab completion
    // =========================================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("toggle", "place", "anim", "help", "status"));
            if (hasAdminPermission(sender)) {
                subs.add("reload");
                subs.add("permission");
            }
            String partial = args[0].toLowerCase();
            for (String s : subs) {
                if (s.startsWith(partial))
                    completions.add(s);
            }

        } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            for (String sub : List.of("all", "place", "anim")) {
                if (sub.startsWith(args[1].toLowerCase()))
                    completions.add(sub);
            }

        } else if (args.length == 2 && args[0].equalsIgnoreCase("permission") && hasAdminPermission(sender)) {
            for (String a : List.of("add", "remove")) {
                if (a.startsWith(args[1].toLowerCase()))
                    completions.add(a);
            }

        } else if (args.length == 3 && args[0].equalsIgnoreCase("permission")
                && args[1].equalsIgnoreCase("remove") && hasAdminPermission(sender)) {
            String partial = args[2].toLowerCase();
            for (String p : config.getUsePermissions()) {
                if (p.toLowerCase().startsWith(partial))
                    completions.add(p);
            }
        }

        return completions;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String prefix() {
        return MessageUtils.color(config.getPrefix());
    }

    private boolean hasAdminPermission(CommandSender sender) {
        for (String perm : config.getAdminPermissions()) {
            if (sender.hasPermission(perm))
                return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void playSound(Player player, String soundName) {
        if (!config.isSoundsEnabled())
            return;
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            try {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(soundName.trim().toLowerCase());
                Sound sound = org.bukkit.Registry.SOUNDS.get(key);
                if (sound != null) {
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}

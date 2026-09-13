package com.cartprac.battleroyalspawn.commands;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.api.GameState;
import com.cartprac.battleroyalspawn.config.Messages;
import com.cartprac.battleroyalspawn.config.Settings;
import com.cartprac.battleroyalspawn.game.GameManager;
import com.cartprac.battleroyalspawn.game.PlayerSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /battleroyale} ({@code /br}) with tab completion for every argument.
 */
public final class BrCommand implements TabExecutor {

    private static final String PERM_PLAY = "cartdrop.play";
    private static final String PERM_ADMIN = "cartdrop.admin";

    private static final List<String> PLAYER_SUBS = List.of("join", "leave");
    private static final List<String> ADMIN_SUBS = List.of("start", "stop", "setreturn", "setpath", "setcenter",
            "altitude", "forcejump", "announce", "state", "reload");

    private final CartDrop plugin;

    public BrCommand(CartDrop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages m = plugin.messages();
        GameManager game = plugin.game();

        if (args.length == 0) {
            if (sender instanceof Player player && sender.hasPermission(PERM_PLAY)) {
                if (game.announce(List.of(player)) == 0) {
                    player.sendMessage(m.get("queue-already"));
                }
            } else {
                sender.sendMessage(m.get("usage", "usage", "/br <join|leave|start|stop|...>"));
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join" -> {
                Player player = requirePlayer(sender, PERM_PLAY);
                if (player != null) {
                    game.join(player);
                }
            }
            case "leave" -> {
                Player player = requirePlayer(sender, PERM_PLAY);
                if (player != null) {
                    game.leave(player);
                }
            }
            case "start" -> {
                if (requirePermission(sender, PERM_ADMIN)) {
                    start(sender, args.length > 1 && args[1].equalsIgnoreCase("force"));
                }
            }
            case "stop" -> {
                if (requirePermission(sender, PERM_ADMIN)) {
                    sender.sendMessage(game.stop("command", true) ? m.get("stop-done") : m.get("stop-nothing"));
                }
            }
            case "setreturn" -> {
                Player player = requirePlayer(sender, PERM_ADMIN);
                if (player != null) {
                    Location l = player.getLocation();
                    FileConfiguration c = plugin.getConfig();
                    c.set("queue.return-location.world", l.getWorld().getName());
                    c.set("queue.return-location.x", l.getX());
                    c.set("queue.return-location.y", l.getY());
                    c.set("queue.return-location.z", l.getZ());
                    c.set("queue.return-location.yaw", (double) l.getYaw());
                    c.set("queue.return-location.pitch", (double) l.getPitch());
                    saveAndApply();
                    player.sendMessage(m.get("set-return"));
                }
            }
            case "setpath" -> {
                Player player = requirePlayer(sender, PERM_ADMIN);
                if (player == null) {
                    return true;
                }
                if (args.length < 2 || !(args[1].equalsIgnoreCase("a") || args[1].equalsIgnoreCase("b"))) {
                    player.sendMessage(m.get("usage", "usage", "/br setpath <a|b>"));
                    return true;
                }
                String point = args[1].toLowerCase(Locale.ROOT);
                Location l = player.getLocation();
                FileConfiguration c = plugin.getConfig();
                c.set("map.world", l.getWorld().getName());
                c.set("map.point-" + point + ".x", l.getBlockX());
                c.set("map.point-" + point + ".z", l.getBlockZ());
                saveAndApply();
                player.sendMessage(m.get("set-path", "point", point.toUpperCase(Locale.ROOT), "x", l.getBlockX(), "z", l.getBlockZ()));
            }
            case "setcenter" -> {
                Player player = requirePlayer(sender, PERM_ADMIN);
                if (player != null) {
                    Location l = player.getLocation();
                    FileConfiguration c = plugin.getConfig();
                    c.set("map.world", l.getWorld().getName());
                    c.set("map.center.x", l.getBlockX());
                    c.set("map.center.z", l.getBlockZ());
                    saveAndApply();
                    player.sendMessage(m.get("set-center", "x", l.getBlockX(), "z", l.getBlockZ()));
                }
            }
            case "altitude" -> {
                if (!requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                int min = -64;
                int max = 4096;
                Integer value = args.length > 1 ? parseInt(args[1]) : null;
                if (value == null || value < min || value > max) {
                    sender.sendMessage(m.get("altitude-invalid", "min", min, "max", max));
                    return true;
                }
                plugin.getConfig().set("map.altitude", value);
                saveAndApply();
                sender.sendMessage(m.get("set-altitude", "altitude", value));
            }
            case "forcejump" -> {
                if (!requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(m.get("usage", "usage", "/br forcejump <player|all>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    sender.sendMessage(m.get("forcejump-done", "count", game.forceJumpAll()));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(m.get("player-not-found", "name", args[1]));
                    return true;
                }
                sender.sendMessage(game.requestJump(target, true)
                        ? m.get("forcejump-done", "count", 1)
                        : m.get("forcejump-none"));
            }
            case "announce" -> {
                if (requirePermission(sender, PERM_ADMIN)) {
                    sender.sendMessage(m.get("announce-sent", "count", game.announce(Bukkit.getOnlinePlayers())));
                }
            }
            case "state" -> {
                if (requirePermission(sender, PERM_ADMIN)) {
                    for (String line : game.debugReport()) {
                        sender.sendMessage(m.prefix() + ChatColor.GRAY + line);
                    }
                }
            }
            case "reload" -> {
                if (!requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                if (game.state() != GameState.IDLE) {
                    sender.sendMessage(m.get("reload-refused", "state", game.state()));
                    return true;
                }
                int warnings = plugin.reloadFromDisk();
                sender.sendMessage(plugin.messages().get("reload-done", "warnings", warnings));
            }
            default -> sender.sendMessage(m.get("unknown"));
        }
        return true;
    }

    private void start(CommandSender sender, boolean force) {
        Messages m = plugin.messages();
        Settings cfg = plugin.settings();
        GameManager game = plugin.game();
        GameState state = game.state();
        if (state == GameState.COUNTDOWN) {
            if (force) {
                game.startCountdown(true);
                sender.sendMessage(m.get("start-skipped"));
            } else {
                sender.sendMessage(m.get("start-already", "state", state));
            }
            return;
        }
        if (state != GameState.IDLE) {
            sender.sendMessage(m.get("start-already", "state", state));
            return;
        }
        int queued = game.queuedPlayers().size();
        if (!force && queued < cfg.minPlayers) {
            sender.sendMessage(m.get("start-not-enough", "queued", queued, "min", cfg.minPlayers));
            return;
        }
        if (game.startCountdown(force)) {
            int seconds = queued >= cfg.maxPlayers ? cfg.countdownShortSeconds : cfg.countdownSeconds;
            sender.sendMessage(m.get("start-now", "seconds", seconds));
        } else {
            sender.sendMessage(m.get("start-cancelled"));
        }
    }

    private void saveAndApply() {
        plugin.saveConfig();
        plugin.applyConfig();
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Player requirePlayer(CommandSender sender, String permission) {
        if (!requirePermission(sender, permission)) {
            return null;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().get("player-only"));
            return null;
        }
        return player;
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(plugin.messages().get("no-permission"));
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if (sender.hasPermission(PERM_PLAY)) {
                addMatching(out, PLAYER_SUBS, prefix);
            }
            if (sender.hasPermission(PERM_ADMIN)) {
                addMatching(out, ADMIN_SUBS, prefix);
            }
            return out;
        }
        if (args.length == 2 && sender.hasPermission(PERM_ADMIN)) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "start" -> addMatching(out, List.of("force"), prefix);
                case "setpath" -> addMatching(out, List.of("a", "b"), prefix);
                case "altitude" -> addMatching(out, List.of("120", "150", "180", "220"), prefix);
                case "forcejump" -> {
                    List<String> names = new ArrayList<>();
                    names.add("all");
                    for (PlayerSession s : plugin.game().sessions()) {
                        if (s.isOnCart()) {
                            names.add(s.name);
                        }
                    }
                    addMatching(out, names, prefix);
                }
                default -> {
                }
            }
        }
        return out;
    }

    private static void addMatching(List<String> out, List<String> options, String prefix) {
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(option);
            }
        }
    }
}

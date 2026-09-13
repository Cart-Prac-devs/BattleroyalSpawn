package com.cartprac.battleroyalspawn.listeners;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

/**
 * Join: pending return teleport, leftover glider cleanup, queue announcement. Quit: session cleanup.
 */
public final class ConnectionListener implements Listener {

    private final CartDrop plugin;

    public ConnectionListener(CartDrop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameManager game = plugin.game();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            game.handleJoin(player);
            if (!game.isManaged(player.getUniqueId()) && game.glider().stripAny(player) > 0) {
                player.setGliding(false);
                plugin.getLogger().warning("Removed a leftover glider from " + player.getName() + " on login");
            }
        });
        if (plugin.settings().announceOnJoin) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    game.announce(List.of(player));
                }
            }, plugin.settings().announceJoinDelayTicks);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.game().handleQuit(event.getPlayer());
    }
}

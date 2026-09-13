package com.cartprac.battleroyalspawn.listeners;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.api.GameState;
import com.cartprac.battleroyalspawn.game.GameManager;
import com.cartprac.battleroyalspawn.util.Tags;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public final class WorldListener implements Listener {

    private final CartDrop plugin;

    public WorldListener(CartDrop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (Tags.isOurs(entity)) {
                entity.remove();
            }
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        GameManager game = plugin.game();
        if (game.mapWorld() != null && event.getWorld().equals(game.mapWorld()) && game.state() != GameState.IDLE) {
            plugin.getLogger().warning("Arena world " + event.getWorld().getName() + " is unloading; stopping the match");
            game.stop("world-unload", true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        plugin.game().handleTeleport(event.getPlayer(), event);
    }
}

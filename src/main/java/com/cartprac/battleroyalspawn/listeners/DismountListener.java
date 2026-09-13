package com.cartprac.battleroyalspawn.listeners;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.util.Tags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;

/**
 * Every way a player can leave a seat (sneak, teleport, seat removed, plugin eject) ends up here.
 * The game decides: keep them seated while the doors are closed, otherwise it is a jump.
 */
public final class DismountListener implements Listener {

    private final CartDrop plugin;

    public DismountListener(CartDrop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Entity seat = event.getDismounted();
        if (!seat.getScoreboardTags().contains(Tags.SEAT)) {
            return;
        }
        if (plugin.game().onDismountAttempt(player)) {
            event.setCancelled(true);
        }
    }
}

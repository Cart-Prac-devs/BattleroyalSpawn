package com.cartprac.battleroyalspawn.listeners;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.util.Tags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;

// every way of leaving a seat ends up here (sneak, tp, seat removed, plugin eject)
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

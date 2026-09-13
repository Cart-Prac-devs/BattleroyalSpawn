package com.cartprac.battleroyalspawn.listeners;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.game.GameManager;
import com.cartprac.battleroyalspawn.game.PlayerSession;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * No fall/void/wall damage while dropping, no damage at all while seated, and two-way protection
 * during the post-landing grace period.
 */
public final class DamageListener implements Listener {

    private final CartDrop plugin;

    public DamageListener(CartDrop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        GameManager game = plugin.game();
        PlayerSession session = game.session(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.isManaged()) {
            if (session.isOnCart()) {
                event.setCancelled(true); // a seated player cannot defend themselves
                return;
            }
            switch (event.getCause()) {
                case FALL, VOID -> event.setCancelled(true);
                case FLY_INTO_WALL -> {
                    if (plugin.settings().cancelWallDamage) {
                        event.setCancelled(true);
                    }
                }
                default -> {
                }
            }
            return;
        }
        if (game.isGraced(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player attacker = attackerOf(event.getDamager());
        if (attacker != null && plugin.game().isGraced(attacker.getUniqueId())) {
            event.setCancelled(true); // grace is no-PvP in both directions
        }
    }

    private static Player attackerOf(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }
}

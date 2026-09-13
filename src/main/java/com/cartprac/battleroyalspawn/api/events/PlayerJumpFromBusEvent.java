package com.cartprac.battleroyalspawn.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired when a player is about to leave the cart and start gliding.
 * <p>
 * Cancelling keeps the player seated, unless {@link #isForced()} is true (end of path, admin
 * command, seat lost), in which case cancellation is ignored.
 */
public class PlayerJumpFromBusEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private final boolean forced;
    private boolean cancelled;

    public PlayerJumpFromBusEvent(Player who, Location location, boolean forced) {
        super(who);
        this.location = location;
        this.forced = forced;
    }

    /** Where the player was when they jumped. */
    public Location getLocation() {
        return location.clone();
    }

    /** True when the plugin ejected the player rather than the player choosing to jump. */
    public boolean isForced() {
        return forced;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

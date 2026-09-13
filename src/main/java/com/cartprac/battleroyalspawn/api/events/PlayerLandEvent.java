package com.cartprac.battleroyalspawn.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired once a player has landed and CartDrop has stopped managing them. The glider has already been
 * removed and the saved chest item restored when this fires. This is the hook for the rest of the game.
 */
public class PlayerLandEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private final int airtimeTicks;
    private final boolean safetyNet;

    public PlayerLandEvent(Player who, Location location, int airtimeTicks, boolean safetyNet) {
        super(who);
        this.location = location;
        this.airtimeTicks = airtimeTicks;
        this.safetyNet = safetyNet;
    }

    /** Where the player landed. */
    public Location getLocation() {
        return location.clone();
    }

    /** Ticks between leaving the cart and the landing being confirmed. */
    public int getAirtimeTicks() {
        return airtimeTicks;
    }

    /** True if the player was put on the ground by the max-drop-seconds safety net. */
    public boolean isSafetyNet() {
        return safetyNet;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

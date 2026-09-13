package com.cartprac.battleroyalspawn.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired after a match has been torn down ({@code /br stop}, API stop, world unload or plugin disable)
 * and the state is back to IDLE. All cart entities are gone and players have been returned.
 */
public class GameResetEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String reason;

    public GameResetEvent(String reason) {
        this.reason = reason;
    }

    /** Short machine-readable reason, for example "command", "api", "disable", "world-unload". */
    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

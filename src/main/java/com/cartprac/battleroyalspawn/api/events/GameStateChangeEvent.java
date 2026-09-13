package com.cartprac.battleroyalspawn.api.events;

import com.cartprac.battleroyalspawn.api.GameState;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired whenever the global {@link GameState} changes.
 * <p>
 * Cancelling is honoured for IDLE -> COUNTDOWN and COUNTDOWN -> BUS (the match will not start).
 * Transitions that are part of cleanup (to DROP, LIVE, RESETTING, IDLE) ignore cancellation, because
 * refusing them would leave entities and players in a half-managed state.
 */
public class GameStateChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameState oldState;
    private final GameState newState;
    private boolean cancelled;

    public GameStateChangeEvent(GameState oldState, GameState newState) {
        this.oldState = oldState;
        this.newState = newState;
    }

    public GameState getOldState() {
        return oldState;
    }

    public GameState getNewState() {
        return newState;
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

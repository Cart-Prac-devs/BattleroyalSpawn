package com.cartprac.battleroyalspawn.api;

/**
 * Global state of the CartDrop match flow.
 * <p>
 * Transitions: IDLE -> COUNTDOWN -> BUS -> DROP -> LIVE -> RESETTING -> IDLE.
 * COUNTDOWN can fall back to IDLE when the queue shrinks below the minimum.
 * RESETTING is transient: it is only visible inside a {@code GameStateChangeEvent}.
 */
public enum GameState {
    /** No match. The queue is open and fills up. */
    IDLE,
    /** Enough players queued; boss bar counts down to departure. */
    COUNTDOWN,
    /** Participants have been moved to the arena world and ride the cart. */
    BUS,
    /** Nobody is aboard any more; some players are still gliding. */
    DROP,
    /** Everyone has landed (or was put down). CartDrop no longer manages players. */
    LIVE,
    /** Cleaning up entities and returning players. */
    RESETTING;

    /** True while a match occupies the arena (COUNTDOWN through LIVE). */
    public boolean isMatchRunning() {
        return this != IDLE;
    }
}

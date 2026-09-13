package com.cartprac.battleroyalspawn.api;

import com.cartprac.battleroyalspawn.game.GameManager;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Public entry point for other plugins.
 * <pre>
 * CartDropAPI api = CartDropAPI.get();
 * if (api.getState() == GameState.LIVE) { ... }
 * </pre>
 * Obtain it after CartDrop is enabled (declare {@code depend: [CartDrop]} in your plugin.yml).
 */
public final class CartDropAPI {

    private static CartDropAPI instance;

    private final GameManager game;

    private CartDropAPI(GameManager game) {
        this.game = game;
    }

    /** Internal: called by CartDrop on enable. */
    public static void init(GameManager game) {
        instance = new CartDropAPI(game);
    }

    /** Internal: called by CartDrop on disable. */
    public static void shutdown() {
        instance = null;
    }

    /** True while CartDrop is enabled. */
    public static boolean isAvailable() {
        return instance != null;
    }

    /**
     * @throws IllegalStateException if CartDrop is not enabled
     */
    public static CartDropAPI get() {
        if (instance == null) {
            throw new IllegalStateException("CartDrop is not enabled");
        }
        return instance;
    }

    /** Current global state. */
    public GameState getState() {
        return game.state();
    }

    /** Online players in the current match (any phase, including landed). Empty when IDLE/COUNTDOWN. */
    public Set<Player> getParticipants() {
        return new LinkedHashSet<>(game.participants());
    }

    /** Online players waiting in the queue. */
    public Set<Player> getQueue() {
        return new LinkedHashSet<>(game.queuedPlayers());
    }

    public boolean isQueued(Player player) {
        return game.isQueued(player.getUniqueId());
    }

    /** True while the player is on (or being placed on) the cart. */
    public boolean isAboard(Player player) {
        return game.isAboard(player.getUniqueId());
    }

    /** True while the player has left the cart and not yet landed. */
    public boolean isInDrop(Player player) {
        return game.isInDrop(player.getUniqueId());
    }

    /** True during the post-landing grace period. */
    public boolean isGraced(Player player) {
        return game.isGraced(player.getUniqueId());
    }

    /**
     * Starts the countdown now, ignoring the minimum player check (same as {@code /br start force}).
     * During a running countdown this skips the remaining time.
     *
     * @return false if a match is already past the countdown, or a plugin cancelled the state change
     */
    public boolean startGame() {
        return game.startCountdown(true);
    }

    /** Ends the match, removes every cart entity and returns all participants. Same as {@code /br stop}. */
    public boolean stopGame() {
        return game.stop("api", true);
    }

    /**
     * Ends the match and cleans up. With {@code returnPlayers = false} participants stay where they are,
     * which is what a game plugin wants when it ends a match that already reached LIVE.
     */
    public boolean stopGame(boolean returnPlayers) {
        return game.stop("api", returnPlayers);
    }

    /** Ejects a seated player as a forced jump. Returns false if they are not on the cart. */
    public boolean forceJump(Player player) {
        return game.requestJump(player, true);
    }

    /** Adds a player to the queue as if they clicked the chat message. */
    public void joinQueue(Player player) {
        game.join(player);
    }

    /** Removes a player from the queue or match (a seated player jumps). */
    public boolean leaveQueue(Player player) {
        return game.leave(player);
    }
}

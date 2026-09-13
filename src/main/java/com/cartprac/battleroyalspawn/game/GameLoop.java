package com.cartprac.battleroyalspawn.game;

import com.cartprac.battleroyalspawn.CartDrop;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

/**
 * The single 1-tick scheduler task. All per-tick work goes through {@link GameManager#tick()}; a thrown
 * exception is logged once and then muted for five seconds so a bug cannot flood the console.
 */
public final class GameLoop extends BukkitRunnable {

    private final CartDrop plugin;
    private final GameManager game;
    private int muteUntil;

    public GameLoop(CartDrop plugin, GameManager game) {
        this.plugin = plugin;
        this.game = game;
    }

    @Override
    public void run() {
        try {
            game.tick();
        } catch (Throwable t) {
            int now = game.currentTick();
            if (now >= muteUntil) {
                muteUntil = now + 100;
                plugin.getLogger().log(Level.SEVERE, "Error in the CartDrop tick loop (further errors muted for 5s)", t);
            }
        }
    }
}

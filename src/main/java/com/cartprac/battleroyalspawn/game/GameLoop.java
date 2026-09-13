package com.cartprac.battleroyalspawn.game;

import com.cartprac.battleroyalspawn.CartDrop;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

// the one repeating task. exceptions get logged once then muted for 5s so the console doesn't explode
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

package com.cartprac.battleroyalspawn.listeners;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.api.GameState;
import com.cartprac.battleroyalspawn.config.Settings;
import com.cartprac.battleroyalspawn.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

// jump key -> requestJump. sneaking also dismounts through vanilla, DismountListener handles that side
public final class JumpListener implements Listener {

    private final CartDrop plugin;

    public JumpListener(CartDrop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking() || plugin.settings().jumpKey != Settings.JumpKey.SNEAK) {
            return;
        }
        GameManager game = plugin.game();
        Player player = event.getPlayer();
        if (game.state() != GameState.BUS || !game.isAboard(player.getUniqueId())) {
            return;
        }
        game.requestJump(player, false); // handles the doors-closed message itself
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || plugin.settings().jumpKey != Settings.JumpKey.RIGHT_CLICK) {
            return;
        }
        GameManager game = plugin.game();
        Player player = event.getPlayer();
        if (game.state() != GameState.BUS || !game.isAboard(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        game.requestJump(player, false);
    }
}

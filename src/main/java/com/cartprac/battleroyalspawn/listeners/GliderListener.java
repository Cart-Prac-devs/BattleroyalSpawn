package com.cartprac.battleroyalspawn.listeners;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.drop.GliderManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

// keeps the elytra on: no clicking/dragging/dropping/swapping it, no rockets, sort out drops on death
public final class GliderListener implements Listener {

    private final CartDrop plugin;

    public GliderListener(CartDrop plugin) {
        this.plugin = plugin;
    }

    private GliderManager glider() {
        return plugin.game().glider();
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        GliderManager glider = glider();
        if (glider.isGlider(event.getCurrentItem()) || glider.isGlider(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
            if (glider.isGlider(player.getInventory().getItem(event.getHotbarButton()))) {
                event.setCancelled(true);
            }
        } else if (event.getClick() == ClickType.SWAP_OFFHAND && glider.isGlider(player.getInventory().getItemInOffHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (glider().isGlider(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (glider().isGlider(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        GliderManager glider = glider();
        if (glider.isGlider(event.getMainHandItem()) || glider.isGlider(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!glider().isWearingGlider(player)) {
            return;
        }
        Material type = item.getType();
        boolean rocket = type == Material.FIREWORK_ROCKET;
        boolean chestArmor = type == Material.ELYTRA || type.name().endsWith("_CHESTPLATE");
        if (rocket || chestArmor) {
            // no rocket boosting, and no right-click armor swap that takes the glider off
            event.setUseItemInHand(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        plugin.game().handleDeath(event.getEntity(), event);
    }
}

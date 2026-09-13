package com.cartprac.battleroyalspawn.drop;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.game.PlayerSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Creates, identifies, equips and removes the drop glider (an unbreakable elytra tagged {@code cartdrop:glider}).
 */
public final class GliderManager {

    private final NamespacedKey key;

    public GliderManager(CartDrop plugin) {
        this.key = new NamespacedKey(plugin, "glider");
    }

    /** A fresh glider item. */
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.ELYTRA);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.setDisplayName(ChatColor.AQUA + "Glider");
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** True if the stack is a CartDrop glider. */
    public boolean isGlider(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public boolean isWearingGlider(Player player) {
        return isGlider(player.getInventory().getChestplate());
    }

    /** Saves the chest slot into the session and puts the glider on. */
    public void equip(Player player, PlayerSession session) {
        if (session.gliderEquipped) {
            return;
        }
        ItemStack chest = player.getInventory().getChestplate();
        session.savedChest = (chest == null || chest.getType().isAir()) ? null : chest.clone();
        session.gliderEquipped = true;
        player.getInventory().setChestplate(create());
    }

    /** Removes every glider from the player and puts the saved chest item back exactly as it was. */
    public void restore(Player player, PlayerSession session) {
        if (!session.gliderEquipped) {
            return;
        }
        stripAny(player);
        player.getInventory().setChestplate(session.savedChest);
        session.savedChest = null;
        session.gliderEquipped = false;
    }

    /** Removes glider items anywhere in the inventory (used for orphan cleanup). Returns how many were removed. */
    public int stripAny(Player player) {
        PlayerInventory inv = player.getInventory();
        int removed = 0;
        if (isGlider(inv.getChestplate())) {
            inv.setChestplate(null);
            removed++;
        }
        if (isGlider(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
            removed++;
        }
        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (isGlider(contents[i])) {
                inv.setItem(i, null);
                removed++;
            }
        }
        return removed;
    }
}

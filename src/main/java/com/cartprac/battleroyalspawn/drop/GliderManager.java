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

// the elytra. unbreakable + pdc tag so we can find it again
public final class GliderManager {

    private final NamespacedKey key;

    public GliderManager(CartDrop plugin) {
        this.key = new NamespacedKey(plugin, "glider");
    }

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

    public void equip(Player player, PlayerSession session) {
        if (session.gliderEquipped) {
            return;
        }
        ItemStack chest = player.getInventory().getChestplate();
        session.savedChest = (chest == null || chest.getType().isAir()) ? null : chest.clone();
        session.gliderEquipped = true;
        player.getInventory().setChestplate(create());
    }

    public void restore(Player player, PlayerSession session) {
        if (!session.gliderEquipped) {
            return;
        }
        stripAny(player);
        player.getInventory().setChestplate(session.savedChest);
        session.savedChest = null;
        session.gliderEquipped = false;
    }

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

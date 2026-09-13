package com.cartprac.battleroyalspawn.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Everything CartDrop remembers about one participant for the duration of a match.
 * Only referenced by UUID so a disconnect can never hold a stale {@link Player}.
 */
public final class PlayerSession {

    /** Per-player phase, independent of the global state. */
    public enum Phase {
        /** Teleported into the arena world, not yet mounted. */
        TRANSFERRING,
        /** Riding their seat on the cart. */
        ABOARD,
        /** Off the cart, glider equipped, not yet landed. */
        DROPPING,
        /** Landed; CartDrop only tracks the grace timer and the return location. */
        LANDED,
        /** Removed from the match (quit, death, teleported away, /br leave). */
        OUT
    }

    public final UUID uuid;
    public final String name;
    public final int seatIndex;

    public Phase phase = Phase.TRANSFERRING;
    /** The invisible armor stand this player rides, or null when not aboard. */
    public ArmorStand seat;
    /** Chest slot content before the glider was equipped (may be null = empty). */
    public ItemStack savedChest;
    public boolean gliderEquipped;
    public boolean gliderStarted;
    public int airtimeTicks;
    public int groundTicks;
    /** Global tick until which the player is invulnerable and cannot PvP; -1 = no grace. */
    public int graceUntilTick = -1;
    /** Where the player stood when the cart picked them up. */
    public Location returnLocation;
    /** Where the player left the cart. */
    public Location jumpLocation;
    public boolean voidRescued;
    public boolean retriedTeleport;
    public final int transferTick;

    public PlayerSession(Player player, int seatIndex, int transferTick) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.seatIndex = seatIndex;
        this.transferTick = transferTick;
    }

    /** The online player, or null if they disconnected. */
    public Player player() {
        return Bukkit.getPlayer(uuid);
    }

    /** True while CartDrop is actively moving or watching this player. */
    public boolean isManaged() {
        return phase == Phase.TRANSFERRING || phase == Phase.ABOARD || phase == Phase.DROPPING;
    }

    /** True while the player is on (or being placed on) the cart. */
    public boolean isOnCart() {
        return phase == Phase.TRANSFERRING || phase == Phase.ABOARD;
    }
}

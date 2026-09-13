package com.cartprac.battleroyalspawn.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

// one player in the current round. uuid only, never hold on to the Player
public final class PlayerSession {

    public enum Phase {
        TRANSFERRING, // teleported to the arena, not mounted yet
        ABOARD,
        DROPPING,
        LANDED,       // only the grace timer + return location matter now
        OUT           // quit / died / tp'd away / left
    }

    public final UUID uuid;
    public final String name;
    public final int seatIndex;

    public Phase phase = Phase.TRANSFERRING;
    public ArmorStand seat;
    public ItemStack savedChest;
    public boolean gliderEquipped;
    public boolean gliderStarted;
    public int airtimeTicks;
    public int groundTicks;
    public int graceUntilTick = -1;
    public Location returnLocation;
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

    public Player player() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isManaged() {
        return phase == Phase.TRANSFERRING || phase == Phase.ABOARD || phase == Phase.DROPPING;
    }

    public boolean isOnCart() {
        return phase == Phase.TRANSFERRING || phase == Phase.ABOARD;
    }
}

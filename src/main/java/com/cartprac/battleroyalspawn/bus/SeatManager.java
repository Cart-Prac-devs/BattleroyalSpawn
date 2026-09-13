package com.cartprac.battleroyalspawn.bus;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.config.Settings;
import com.cartprac.battleroyalspawn.game.PlayerSession;
import com.cartprac.battleroyalspawn.util.LocationUtil;
import com.cartprac.battleroyalspawn.util.Tags;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * One invisible armor stand per player, laid out in rows inside the cart and dragged along every tick.
 * <p>
 * Seats are moved by setting their velocity to the exact delta to their target, which self-corrects drift.
 * Spigot's {@code Entity#teleport} silently refuses entities that have passengers, so teleporting a
 * mounted seat is not an option here (Paper offers {@code TeleportFlag.EntityState.RETAIN_PASSENGERS}).
 */
public final class SeatManager {

    private final CartDrop plugin;

    public SeatManager(CartDrop plugin) {
        this.plugin = plugin;
    }

    /** Cart-local offset of seat {@code index} when {@code total} seats are laid out. */
    public Vector localOffset(int index, int total) {
        Settings s = plugin.settings();
        int perRow = s.seatsPerRow;
        int rows = Math.max(1, (total + perRow - 1) / perRow);
        int row = index / perRow;
        int col = index % perRow;
        int inRow = Math.max(1, Math.min(perRow, total - row * perRow));
        double x = (col - (inRow - 1) / 2.0) * s.seatSpacing;
        double z = ((rows - 1) / 2.0 - row) * s.seatRowSpacing;
        return new Vector(x, s.seatYOffset, z);
    }

    /** World position of seat {@code index} for a cart at {@code origin}. */
    public Location target(Location origin, int index, int total) {
        Vector off = LocationUtil.rotateYaw(localOffset(index, total), origin.getYaw());
        return new Location(origin.getWorld(),
                origin.getX() + off.getX(), origin.getY() + off.getY(), origin.getZ() + off.getZ(),
                origin.getYaw(), 0f);
    }

    /** Spawns a seat entity at the given location. */
    public ArmorStand spawn(Location at) {
        return at.getWorld().spawn(at, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setArms(false);
            // Marker must stay OFF and gravity ON: an armor stand with either set skips its physics tick and
            // ignores velocity entirely, and velocity is the only Spigot-safe way to move a mounted seat.
            stand.setMarker(false);
            stand.setGravity(true);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setPersistent(false);
            stand.setCollidable(false);
            stand.setCanPickupItems(false);
            stand.setRemoveWhenFarAway(false);
            stand.addScoreboardTag(Tags.SEAT);
        });
    }

    /** Mounts the player. Returns false if the server refused (different world, dead player, ...). */
    public boolean mount(ArmorStand seat, Player player) {
        return seat.addPassenger(player);
    }

    /** Drags the seat to its target this tick. */
    public void move(ArmorStand seat, Location target) {
        Location current = seat.getLocation();
        seat.setVelocity(new Vector(
                target.getX() - current.getX(),
                target.getY() - current.getY(),
                target.getZ() - current.getZ()));
    }

    /** Removes the session's seat entity immediately and clears the reference. */
    public void remove(PlayerSession session) {
        ArmorStand seat = session.seat;
        session.seat = null;
        if (seat != null && seat.isValid()) {
            seat.eject();
            seat.remove();
        }
    }
}

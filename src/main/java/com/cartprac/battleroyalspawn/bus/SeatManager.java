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

// one invisible armor stand per player, dragged along every tick with velocity.
// can't teleport them: spigot's Entity#teleport returns false if the entity has a passenger
// (paper has TeleportFlag.EntityState.RETAIN_PASSENGERS for this)
public final class SeatManager {

    private final CartDrop plugin;

    public SeatManager(CartDrop plugin) {
        this.plugin = plugin;
    }

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

    public Location target(Location origin, int index, int total) {
        Vector off = LocationUtil.rotateYaw(localOffset(index, total), origin.getYaw());
        return new Location(origin.getWorld(),
                origin.getX() + off.getX(), origin.getY() + off.getY(), origin.getZ() + off.getZ(),
                origin.getYaw(), 0f);
    }

    public ArmorStand spawn(Location at) {
        return at.getWorld().spawn(at, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setArms(false);
            // marker off + gravity on or the stand ignores velocity completely (ArmorStand#hasPhysics)
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

    public boolean mount(ArmorStand seat, Player player) {
        return seat.addPassenger(player);
    }

    public void move(ArmorStand seat, Location target) {
        Location current = seat.getLocation();
        seat.setVelocity(new Vector(
                target.getX() - current.getX(),
                target.getY() - current.getY(),
                target.getZ() - current.getZ()));
    }

    public void remove(PlayerSession session) {
        ArmorStand seat = session.seat;
        session.seat = null;
        if (seat != null && seat.isValid()) {
            seat.eject();
            seat.remove();
        }
    }
}

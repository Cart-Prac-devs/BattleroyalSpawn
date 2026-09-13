package com.cartprac.battleroyalspawn.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Geometry helpers for the cart's local coordinate frame and for finding safe ground.
 */
public final class LocationUtil {

    private LocationUtil() {
    }

    /**
     * Rotates a cart-local offset (x sideways, y up, z forward) into a world offset for a cart facing {@code yawDeg}.
     * Uses Minecraft's yaw convention: yaw 0 faces +Z, yaw 90 faces -X.
     */
    public static Vector rotateYaw(Vector local, float yawDeg) {
        double t = Math.toRadians(-yawDeg);
        double cos = Math.cos(t);
        double sin = Math.sin(t);
        return new Vector(
                local.getX() * cos + local.getZ() * sin,
                local.getY(),
                -local.getX() * sin + local.getZ() * cos);
    }

    /** Yaw (degrees, Minecraft convention) that faces along the given horizontal direction. */
    public static float yawOf(Vector direction) {
        Location probe = new Location(null, 0, 0, 0);
        probe.setDirection(direction);
        return probe.getYaw();
    }

    /**
     * Location on top of the highest motion-blocking block in the column, or {@code null} if the column is void.
     */
    public static Location highestSafe(World world, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int y = world.getHighestBlockYAt(bx, bz);
        if (y < world.getMinHeight()) {
            return null;
        }
        return new Location(world, bx + 0.5, y + 1.0, bz + 0.5);
    }

    /** Packs chunk coordinates into one long key. */
    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }
}

package com.cartprac.battleroyalspawn.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public final class LocationUtil {

    private LocationUtil() {
    }

    // cart local (x sideways, y up, z forward) -> world offset. mc yaw: 0 = +z, 90 = -x
    public static Vector rotateYaw(Vector local, float yawDeg) {
        double t = Math.toRadians(-yawDeg);
        double cos = Math.cos(t);
        double sin = Math.sin(t);
        return new Vector(
                local.getX() * cos + local.getZ() * sin,
                local.getY(),
                -local.getX() * sin + local.getZ() * cos);
    }

    public static float yawOf(Vector direction) {
        Location probe = new Location(null, 0, 0, 0);
        probe.setDirection(direction);
        return probe.getYaw();
    }

    // null if the column is all void
    public static Location highestSafe(World world, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int y = world.getHighestBlockYAt(bx, bz);
        if (y < world.getMinHeight()) {
            return null;
        }
        return new Location(world, bx + 0.5, y + 1.0, bz + 0.5);
    }

    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }
}

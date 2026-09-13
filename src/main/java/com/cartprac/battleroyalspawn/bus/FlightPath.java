package com.cartprac.battleroyalspawn.bus;

import com.cartprac.battleroyalspawn.config.Settings;
import com.cartprac.battleroyalspawn.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Random;

// straight line A -> B at a fixed height
public final class FlightPath {

    private final World world;
    private final Vector start;
    private final Vector end;
    private final Vector direction;
    private final double length;
    private final double speed;
    private final float yaw;
    private double progress;

    private FlightPath(World world, Vector start, Vector end, double speed) {
        this.world = world;
        this.start = start;
        this.end = end;
        Vector delta = end.clone().subtract(start);
        this.length = delta.length();
        this.direction = length > 0 ? delta.multiply(1.0 / length) : new Vector(0, 0, 1);
        this.speed = speed;
        this.yaw = LocationUtil.yawOf(direction);
    }

    public static FlightPath build(Settings s, World world, Random random) {
        int altitude = clampAltitude(s.altitude, world);
        Vector a;
        Vector b;
        if (s.randomBearing) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dx = Math.cos(angle) * s.radius;
            double dz = Math.sin(angle) * s.radius;
            a = new Vector(s.centerX - dx, altitude, s.centerZ - dz);
            b = new Vector(s.centerX + dx, altitude, s.centerZ + dz);
        } else {
            a = new Vector(s.pointAX, altitude, s.pointAZ);
            b = new Vector(s.pointBX, altitude, s.pointBZ);
        }
        return new FlightPath(world, a, b, s.speed);
    }

    public static int clampAltitude(int altitude, World world) {
        return Math.max(world.getMinHeight() + 16, Math.min(altitude, world.getMaxHeight() - 10));
    }

    public void advance() {
        progress = Math.min(length, progress + speed);
    }

    public Vector position() {
        return start.clone().add(direction.clone().multiply(progress));
    }

    public Location location() {
        Vector p = position();
        return new Location(world, p.getX(), p.getY(), p.getZ(), yaw, 0f);
    }

    public double fraction() {
        return length <= 0 ? 1.0 : progress / length;
    }

    public boolean isFinished() {
        return progress >= length;
    }

    public double ticksUntil(double fraction) {
        double remaining = fraction * length - progress;
        return remaining <= 0 ? 0 : remaining / speed;
    }

    public Vector direction() {
        return direction.clone();
    }

    public float getYaw() {
        return yaw;
    }

    public World getWorld() {
        return world;
    }

    public Vector getStart() {
        return start.clone();
    }

    public Vector getEnd() {
        return end.clone();
    }

    public double getLength() {
        return length;
    }

    public double getProgress() {
        return progress;
    }

    public double getSpeed() {
        return speed;
    }

    public String describe() {
        return String.format(Locale.ROOT, "%s (%.0f, %.0f) -> (%.0f, %.0f) at Y=%.0f, %.0f blocks, %.2f blocks/tick (~%.0fs)",
                world.getName(), start.getX(), start.getZ(), end.getX(), end.getZ(), start.getY(), length, speed,
                speed > 0 ? length / speed / 20.0 : 0);
    }
}

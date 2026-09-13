package com.cartprac.battleroyalspawn.bus;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.config.RigSpec;
import com.cartprac.battleroyalspawn.config.Settings;
import com.cartprac.battleroyalspawn.util.Tags;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The visible cart: display entities that all sit at the cart origin and carry their own local offset in
 * their transformation. With billboard FIXED the client applies the entity yaw before the transformation,
 * so teleporting every part to the same origin (with the path yaw) rotates the whole rig as one piece.
 */
public final class BusRig {

    private final CartDrop plugin;
    private final List<Display> parts = new ArrayList<>();
    private World world;

    public BusRig(CartDrop plugin) {
        this.plugin = plugin;
    }

    public boolean isSpawned() {
        return !parts.isEmpty();
    }

    public int partCount() {
        return parts.size();
    }

    /** Spawns every part at the origin. Any previous rig is removed first. */
    public void spawn(Location origin) {
        despawn();
        world = origin.getWorld();
        RigSpec spec = plugin.settings().rig;
        if (spec.cart.enabled) {
            parts.add(spawnItem(origin, spec.cart));
        }
        if (spec.banner.enabled) {
            parts.add(spawnItem(origin, spec.banner));
        }
        for (RigSpec.BlockPart block : spec.blocks) {
            parts.add(spawnBlock(origin, block));
        }
    }

    private ItemDisplay spawnItem(Location origin, RigSpec.ItemPart part) {
        return world.spawn(origin, ItemDisplay.class, d -> {
            common(d);
            d.setItemStack(new ItemStack(part.item));
            d.setItemDisplayTransform(part.mode);
            Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(part.yaw));
            d.setTransformation(new Transformation(
                    new Vector3f((float) part.offset.getX(), (float) part.offset.getY(), (float) part.offset.getZ()),
                    rotation,
                    new Vector3f(part.scale, part.scale, part.scale),
                    new Quaternionf()));
        });
    }

    private BlockDisplay spawnBlock(Location origin, RigSpec.BlockPart part) {
        return world.spawn(origin, BlockDisplay.class, d -> {
            common(d);
            d.setBlock(part.block.createBlockData());
            // A block model spans 0..1 from its corner; shift so `offset` is the centre of the bottom face.
            Vector3f translation = new Vector3f(
                    (float) (part.offset.getX() - part.size.getX() / 2.0),
                    (float) part.offset.getY(),
                    (float) (part.offset.getZ() - part.size.getZ() / 2.0));
            d.setTransformation(new Transformation(
                    translation,
                    new Quaternionf(),
                    new Vector3f((float) part.size.getX(), (float) part.size.getY(), (float) part.size.getZ()),
                    new Quaternionf()));
        });
    }

    private void common(Display d) {
        d.setPersistent(false);
        d.setBillboard(Display.Billboard.FIXED);
        // One-tick teleport interpolation: the client slides the part to each new position instead of snapping.
        d.setTeleportDuration(1);
        d.setInterpolationDuration(0);
        d.setInterpolationDelay(0);
        d.setInvulnerable(true);
        d.setSilent(true);
        d.setGravity(false);
        d.addScoreboardTag(Tags.BUS);
    }

    /** Teleports every part to the new origin (same yaw for all). */
    public void move(Location origin) {
        for (Display d : parts) {
            if (d.isValid()) {
                d.teleport(origin);
            }
        }
    }

    /** Particle puff a few blocks behind the cart. */
    public void trail(Location origin, Vector direction) {
        Settings s = plugin.settings();
        if (!s.trailEnabled || s.trailCount <= 0 || world == null) {
            return;
        }
        Location behind = origin.clone().subtract(direction.clone().multiply(4.0));
        world.spawnParticle(s.trailParticle, behind, s.trailCount, 0.8, 0.3, 0.8, 0.01);
    }

    /** Removes every part. Safe to call when nothing is spawned. */
    public void despawn() {
        for (Display d : parts) {
            if (d.isValid()) {
                d.remove();
            }
        }
        parts.clear();
        world = null;
    }
}

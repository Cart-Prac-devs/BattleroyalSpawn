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

// the visible cart. every display sits at the cart origin and has its offset baked into the transformation,
// so teleporting all of them to the same spot with the path yaw rotates the whole thing together
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
            // block models go 0..1 from the corner, shift so offset = centre of the bottom face
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
        // 1 tick lerp so it doesn't stutter
        d.setTeleportDuration(1);
        d.setInterpolationDuration(0);
        d.setInterpolationDelay(0);
        d.setInvulnerable(true);
        d.setSilent(true);
        d.setGravity(false);
        d.addScoreboardTag(Tags.BUS);
    }

    public void move(Location origin) {
        for (Display d : parts) {
            if (d.isValid()) {
                d.teleport(origin);
            }
        }
    }

    public void trail(Location origin, Vector direction) {
        Settings s = plugin.settings();
        if (!s.trailEnabled || s.trailCount <= 0 || world == null) {
            return;
        }
        Location behind = origin.clone().subtract(direction.clone().multiply(4.0));
        world.spawnParticle(s.trailParticle, behind, s.trailCount, 0.8, 0.3, 0.8, 0.01);
    }

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

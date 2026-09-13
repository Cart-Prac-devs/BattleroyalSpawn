package com.cartprac.battleroyalspawn.drop;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.bus.FlightPath;
import com.cartprac.battleroyalspawn.config.Settings;
import com.cartprac.battleroyalspawn.game.PlayerSession;
import com.cartprac.battleroyalspawn.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Per-tick logic for one gliding player: auto-glide, optional speed cap, void guard, landing confirmation
 * and the max-drop-seconds safety net.
 */
public final class LandingDetector {

    /** What the game does when a player is done dropping. */
    public interface Callback {
        void landed(PlayerSession session, Player player, boolean safetyNet);

        void out(PlayerSession session, Player player, String reason);
    }

    private final CartDrop plugin;

    public LandingDetector(CartDrop plugin) {
        this.plugin = plugin;
    }

    public void tick(PlayerSession s, Player p, Callback callback) {
        Settings cfg = plugin.settings();
        World world = p.getWorld();
        Location loc = p.getLocation();
        s.airtimeTicks++;

        // Void guard: lift them back to altitude once; a second fall means the column really is void.
        if (loc.getY() < world.getMinHeight()) {
            if (!s.voidRescued) {
                s.voidRescued = true;
                int altitude = FlightPath.clampAltitude(cfg.altitude, world);
                Location up = new Location(world, loc.getX(), altitude, loc.getZ(), loc.getYaw(), 0f);
                plugin.game().teleportInternal(p, up);
                p.setFallDistance(0);
                p.setVelocity(new Vector(0, 0, 0));
                s.gliderStarted = false;
                p.sendMessage(plugin.messages().get("void-rescue"));
                plugin.getLogger().warning(p.getName() + " fell into the void at " + loc.getBlockX() + ", " + loc.getBlockZ()
                        + " and was lifted back up. Check the flight path config.");
                return;
            }
            Location safe = LocationUtil.highestSafe(world, loc.getX(), loc.getZ());
            if (safe == null) {
                callback.out(s, p, "void");
                return;
            }
            plugin.game().teleportInternal(p, safe);
            callback.landed(s, p, true);
            return;
        }

        boolean grounded = isGrounded(p, loc);
        if (grounded) {
            s.groundTicks++;
        } else {
            s.groundTicks = 0;
            if (!p.isGliding()) {
                p.setGliding(true);
            }
            if (p.isGliding() && !s.gliderStarted) {
                s.gliderStarted = true;
                plugin.game().hud().sound(p, cfg.soundGlideStart);
            }
            // Velocity clamping in the glide loop can feel rubbery; it is off by default and meant for tuning.
            if (cfg.glideCapEnabled && p.isGliding()) {
                Vector v = p.getVelocity();
                double horizontal = Math.hypot(v.getX(), v.getZ());
                if (horizontal > cfg.glideCapMax) {
                    double f = cfg.glideCapMax / horizontal;
                    p.setVelocity(new Vector(v.getX() * f, v.getY(), v.getZ() * f));
                }
            }
        }

        if (grounded && s.groundTicks >= cfg.landingConfirmTicks) {
            callback.landed(s, p, false);
            return;
        }

        if (s.airtimeTicks >= cfg.maxDropSeconds * 20) {
            Location safe = LocationUtil.highestSafe(world, loc.getX(), loc.getZ());
            if (safe != null) {
                plugin.game().teleportInternal(p, safe);
            }
            callback.landed(s, p, true);
        }
    }

    /** On the ground, in water, or caught in leaves/cobweb/climbables, and no longer gliding. */
    public static boolean isGrounded(Player p, Location loc) {
        if (p.isGliding()) {
            return false;
        }
        // Player#isOnGround is deprecated as client-reported; the block checks below back it up.
        if (((Entity) p).isOnGround() || p.isInWater()) {
            return true;
        }
        Block feet = loc.getBlock();
        Material at = feet.getType();
        if (Tag.LEAVES.isTagged(at) || Tag.CLIMBABLE.isTagged(at) || at == Material.COBWEB || at == Material.POWDER_SNOW) {
            return true;
        }
        Material below = feet.getRelative(BlockFace.DOWN).getType();
        return below.isSolid() && p.getVelocity().getY() > -0.1 && (loc.getY() - loc.getBlockY()) < 0.02;
    }
}

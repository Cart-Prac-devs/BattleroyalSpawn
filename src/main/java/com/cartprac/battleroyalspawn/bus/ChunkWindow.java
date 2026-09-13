package com.cartprac.battleroyalspawn.bus;

import com.cartprac.battleroyalspawn.util.LocationUtil;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

// keeps a few chunks in front of the cart loaded with plugin chunk tickets. only recalculated when the
// cart enters a new chunk
public final class ChunkWindow {

    private final Plugin plugin;
    private final Set<Long> held = new HashSet<>();
    private final Set<Long> wanted = new HashSet<>();
    private World world;
    private int ahead;
    private int behind;
    private int lastCx = Integer.MIN_VALUE;
    private int lastCz = Integer.MIN_VALUE;

    public ChunkWindow(Plugin plugin) {
        this.plugin = plugin;
    }

    public void begin(World world, Vector position, Vector direction, int ahead, int behind) {
        release();
        this.world = world;
        this.ahead = ahead;
        this.behind = behind;
        update(position, direction, true);
    }

    public void update(Vector position, Vector direction) {
        update(position, direction, false);
    }

    private void update(Vector position, Vector direction, boolean syncLoad) {
        if (world == null) {
            return;
        }
        int cx = ((int) Math.floor(position.getX())) >> 4;
        int cz = ((int) Math.floor(position.getZ())) >> 4;
        if (cx == lastCx && cz == lastCz) {
            return;
        }
        lastCx = cx;
        lastCz = cz;

        wanted.clear();
        for (double d = -behind * 16.0; d <= ahead * 16.0; d += 8.0) {
            int sx = ((int) Math.floor(position.getX() + direction.getX() * d)) >> 4;
            int sz = ((int) Math.floor(position.getZ() + direction.getZ() * d)) >> 4;
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    wanted.add(LocationUtil.chunkKey(sx + ox, sz + oz));
                }
            }
        }
        for (long key : wanted) {
            if (held.add(key)) {
                int kx = (int) (key >> 32);
                int kz = (int) key;
                world.addPluginChunkTicket(kx, kz, plugin);
                if (syncLoad) {
                    world.getChunkAt(kx, kz);
                }
            }
        }
        held.removeIf(key -> {
            if (wanted.contains(key)) {
                return false;
            }
            long k = key;
            world.removePluginChunkTicket((int) (k >> 32), (int) k, plugin);
            return true;
        });
    }

    public void release() {
        if (world != null) {
            world.removePluginChunkTickets(plugin);
        }
        held.clear();
        wanted.clear();
        world = null;
        lastCx = Integer.MIN_VALUE;
        lastCz = Integer.MIN_VALUE;
    }

    public int heldCount() {
        return held.size();
    }
}

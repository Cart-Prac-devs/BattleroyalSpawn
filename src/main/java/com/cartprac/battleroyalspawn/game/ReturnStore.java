package com.cartprac.battleroyalspawn.game;

import com.cartprac.battleroyalspawn.CartDrop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

// people who logged out mid round get teleported back when they come back. can't cross-world tp in the
// quit event. saved to returns.yml so it survives a restart
public final class ReturnStore {

    private record Entry(String world, double x, double y, double z, float yaw, float pitch) {
        Location toLocation() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z, yaw, pitch);
        }
    }

    private final CartDrop plugin;
    private final File file;
    private final Map<UUID, Entry> pending = new HashMap<>();

    public ReturnStore(CartDrop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "returns.yml");
    }

    public void load() {
        pending.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection sec = yaml.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                pending.put(uuid, new Entry(sec.getString("world", "world"), sec.getDouble("x"), sec.getDouble("y"),
                        sec.getDouble("z"), (float) sec.getDouble("yaw"), (float) sec.getDouble("pitch")));
            } catch (IllegalArgumentException ignored) {
                // not a UUID key; skip
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Entry> e : pending.entrySet()) {
            String base = e.getKey().toString() + ".";
            Entry v = e.getValue();
            yaml.set(base + "world", v.world());
            yaml.set(base + "x", v.x());
            yaml.set(base + "y", v.y());
            yaml.set(base + "z", v.z());
            yaml.set(base + "yaw", (double) v.yaw());
            yaml.set(base + "pitch", (double) v.pitch());
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create the data folder for returns.yml");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not save returns.yml", ex);
        }
    }

    public void put(UUID uuid, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        pending.put(uuid, new Entry(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
        save();
    }

    public boolean has(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public Location take(UUID uuid) {
        Entry e = pending.remove(uuid);
        if (e == null) {
            return null;
        }
        save();
        return e.toLocation();
    }

    public int size() {
        return pending.size();
    }
}

package com.cartprac.battleroyalspawn.config;

import com.cartprac.battleroyalspawn.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Typed, validated view of config.yml. Every value is checked on construction; invalid values are logged
 * as warnings in the form {@code config.yml: <path> must be <rule>, got <value>. Using default <default>.}
 * and replaced by the default, so constructing this class never throws.
 */
public final class Settings {

    /** Where players are sent when they leave the arena. */
    public enum ReturnMode { PREVIOUS, LOCATION }

    /** How a seated player triggers the jump. */
    public enum JumpKey { SNEAK, RIGHT_CLICK, JUMP }

    /** Boss bar appearance for one phase. */
    public static final class BarSpec {
        public final String title;
        public final BarColor color;
        public final BarStyle style;

        BarSpec(String title, BarColor color, BarStyle style) {
            this.title = title;
            this.color = color;
            this.style = style;
        }
    }

    /** A particle burst. */
    public static final class ParticleSpec {
        public final Particle particle;
        public final int count;

        ParticleSpec(Particle particle, int count) {
            this.particle = particle;
            this.count = count;
        }
    }

    private final FileConfiguration c;
    private final Logger log;
    private final List<String> warnings = new ArrayList<>();

    // queue
    public final int minPlayers;
    public final int maxPlayers;
    public final int countdownSeconds;
    public final int countdownShortSeconds;
    public final Set<Integer> announceSeconds;
    public final boolean announceOnJoin;
    public final int announceJoinDelayTicks;
    public final int announceIntervalSeconds;
    public final ReturnMode returnMode;
    public final String returnWorld;
    public final double returnX;
    public final double returnY;
    public final double returnZ;
    public final float returnYaw;
    public final float returnPitch;

    // map
    public final String mapWorld;
    public final double pointAX;
    public final double pointAZ;
    public final double pointBX;
    public final double pointBZ;
    public final boolean randomBearing;
    public final double centerX;
    public final double centerZ;
    public final double radius;
    public final int altitude;

    // bus
    public final double speed;
    public final double doorOpenAfterSeconds;
    public final double doorCloseAt;
    public final JumpKey jumpKey;
    public final double jumpForward;
    public final double jumpDown;
    public final int mountDelayTicks;
    public final int chunkAhead;
    public final int chunkBehind;
    public final double seatSpacing;
    public final int seatsPerRow;
    public final double seatRowSpacing;
    public final double seatYOffset;
    public final RigSpec rig;
    public final boolean trailEnabled;
    public final Particle trailParticle;
    public final int trailCount;

    // drop
    public final int maxDropSeconds;
    public final int landingConfirmTicks;
    public final int graceSeconds;
    public final boolean cancelWallDamage;
    public final boolean glideCapEnabled;
    public final double glideCapMax;

    // hud
    public final BarSpec barCountdown;
    public final BarSpec barBus;
    public final BarSpec barDrop;
    public final boolean actionBar;
    public final int titleFadeIn;
    public final int titleStay;
    public final int titleFadeOut;
    public final ParticleSpec jumpParticles;
    public final ParticleSpec landingParticles;

    // sounds (null = disabled)
    public final Sound soundQueueJoin;
    public final Sound soundCountdownTick;
    public final Sound soundCountdownFinal;
    public final Sound soundDepart;
    public final Sound soundDoorsOpen;
    public final Sound soundJump;
    public final Sound soundGlideStart;
    public final Sound soundLanding;
    public final Sound soundForcedDrop;

    public Settings(FileConfiguration config, Logger log) {
        this.c = config;
        this.log = log;

        // ---- queue ----
        int min = intAt("queue.min-players", 2, 1, 1000);
        int max = intAt("queue.max-players", 60, 1, 1000);
        if (max < min) {
            warn("queue.max-players", "must be >= queue.min-players (" + min + ")", max, min);
            max = min;
        }
        minPlayers = min;
        maxPlayers = max;
        int cd = intAt("queue.countdown-seconds", 30, 1, 3600);
        int cdShort = intAt("queue.countdown-short-seconds", 10, 1, 3600);
        if (cdShort > cd) {
            warn("queue.countdown-short-seconds", "must be <= queue.countdown-seconds (" + cd + ")", cdShort, cd);
            cdShort = cd;
        }
        countdownSeconds = cd;
        countdownShortSeconds = cdShort;
        announceSeconds = intSet("queue.announce-seconds", List.of(30, 20, 10, 5, 4, 3, 2, 1));
        announceOnJoin = bool("queue.announce-on-join", true);
        announceJoinDelayTicks = intAt("queue.announce-on-join-delay-ticks", 40, 0, 6000);
        announceIntervalSeconds = intAt("queue.announce-interval-seconds", 0, 0, 86400);
        returnMode = enumAt("queue.return-mode", ReturnMode.class, ReturnMode.PREVIOUS);
        returnWorld = str("queue.return-location.world", "world");
        returnX = dbl("queue.return-location.x", 0.5, -3.0e7, 3.0e7);
        returnY = dbl("queue.return-location.y", 80.0, -1024, 4096);
        returnZ = dbl("queue.return-location.z", 0.5, -3.0e7, 3.0e7);
        returnYaw = (float) dbl("queue.return-location.yaw", 0.0, -360, 360);
        returnPitch = (float) dbl("queue.return-location.pitch", 0.0, -90, 90);

        // ---- map ----
        mapWorld = str("map.world", "arena");
        double ax = dbl("map.point-a.x", -400, -3.0e7, 3.0e7);
        double az = dbl("map.point-a.z", -400, -3.0e7, 3.0e7);
        double bx = dbl("map.point-b.x", 400, -3.0e7, 3.0e7);
        double bz = dbl("map.point-b.z", 400, -3.0e7, 3.0e7);
        if (ax == bx && az == bz) {
            warn("map.point-b", "must differ from map.point-a", "(" + bx + ", " + bz + ")", "(" + (ax + 200) + ", " + (az + 200) + ")");
            bx = ax + 200;
            bz = az + 200;
        }
        pointAX = ax;
        pointAZ = az;
        pointBX = bx;
        pointBZ = bz;
        randomBearing = bool("map.random-bearing", false);
        centerX = dbl("map.center.x", 0, -3.0e7, 3.0e7);
        centerZ = dbl("map.center.z", 0, -3.0e7, 3.0e7);
        radius = dbl("map.radius", 500, 16, 1.0e6);
        altitude = intAt("map.altitude", 180, -64, 4096);

        // ---- bus ----
        speed = dbl("bus.speed", 0.6, 0.01, 20);
        doorOpenAfterSeconds = dbl("bus.door-open-after-seconds", 3, 0, 600);
        doorCloseAt = dbl("bus.door-close-at", 0.9, 0.1, 1.0);
        JumpKey key = enumAt("bus.jump-key", JumpKey.class, JumpKey.SNEAK);
        if (key == JumpKey.JUMP) {
            // Spigot exposes no input packets for seated players; Paper's PlayerInputEvent would be needed.
            warn("bus.jump-key", "JUMP cannot be detected on Spigot (requires Paper PlayerInputEvent)", key, JumpKey.SNEAK);
            key = JumpKey.SNEAK;
        }
        jumpKey = key;
        jumpForward = dbl("bus.jump-velocity.forward", 0.4, 0, 4);
        jumpDown = dbl("bus.jump-velocity.down", 0.2, 0, 4);
        mountDelayTicks = intAt("bus.mount-delay-ticks", 2, 0, 100);
        chunkAhead = intAt("bus.chunk-window-ahead", 4, 1, 16);
        chunkBehind = intAt("bus.chunk-window-behind", 1, 0, 16);
        seatSpacing = dbl("bus.seats.spacing", 1.0, 0.1, 10);
        seatsPerRow = intAt("bus.seats.per-row", 10, 1, 100);
        seatRowSpacing = dbl("bus.seats.row-spacing", 1.2, 0.1, 10);
        seatYOffset = dbl("bus.seats.y-offset", 0.0, -10, 10);
        rig = loadRig();
        trailEnabled = bool("bus.trail.enabled", true);
        trailParticle = particle("bus.trail.particle", Particle.CLOUD);
        trailCount = intAt("bus.trail.count", 3, 0, 100);

        // ---- drop ----
        maxDropSeconds = intAt("drop.max-drop-seconds", 60, 5, 3600);
        landingConfirmTicks = intAt("drop.landing-confirm-ticks", 5, 1, 200);
        graceSeconds = intAt("drop.grace-seconds", 3, 0, 600);
        cancelWallDamage = bool("drop.cancel-wall-damage", true);
        glideCapEnabled = bool("drop.glide-speed-cap.enabled", false);
        glideCapMax = dbl("drop.glide-speed-cap.max-horizontal", 1.6, 0.1, 20);

        // ---- hud ----
        barCountdown = bar("hud.boss-bar.countdown", "&eBattle Royale starts in &f{seconds}s", BarColor.YELLOW, BarStyle.SOLID);
        barBus = bar("hud.boss-bar.bus", "&6Forced drop in &f{seconds}s", BarColor.RED, BarStyle.SEGMENTED_10);
        barDrop = bar("hud.boss-bar.drop", "&a{landed}&7/&a{total} &7landed", BarColor.GREEN, BarStyle.SOLID);
        actionBar = bool("hud.action-bar", true);
        titleFadeIn = intAt("hud.title.fade-in", 0, 0, 200);
        titleStay = intAt("hud.title.stay", 20, 1, 1200);
        titleFadeOut = intAt("hud.title.fade-out", 10, 0, 200);
        jumpParticles = particles("hud.particles.jump", Particle.CLOUD, 20);
        landingParticles = particles("hud.particles.landing", Particle.POOF, 15);

        // ---- sounds ----
        soundQueueJoin = sound("sounds.queue-join", "BLOCK_NOTE_BLOCK_PLING");
        soundCountdownTick = sound("sounds.countdown-tick", "BLOCK_NOTE_BLOCK_HAT");
        soundCountdownFinal = sound("sounds.countdown-final", "BLOCK_NOTE_BLOCK_BELL");
        soundDepart = sound("sounds.depart", "ENTITY_ENDER_DRAGON_FLAP");
        soundDoorsOpen = sound("sounds.doors-open", "BLOCK_IRON_DOOR_OPEN");
        soundJump = sound("sounds.jump", "ENTITY_ENDER_DRAGON_FLAP");
        soundGlideStart = sound("sounds.glide-start", "ITEM_ELYTRA_FLYING");
        soundLanding = sound("sounds.landing", "BLOCK_GRASS_STEP");
        soundForcedDrop = sound("sounds.forced-drop", "ENTITY_GENERIC_EXPLODE");
    }

    /** Warnings produced while loading, in order. */
    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    /** The configured fixed return location, or {@code null} if its world is not loaded. */
    public Location configuredReturnLocation() {
        World world = Bukkit.getWorld(returnWorld);
        if (world == null) {
            return null;
        }
        return new Location(world, returnX, returnY, returnZ, returnYaw, returnPitch);
    }

    /** Ticks after departure before jumping is allowed. */
    public int doorOpenAfterTicks() {
        return (int) Math.round(doorOpenAfterSeconds * 20.0);
    }

    // ------------------------------------------------------------------ helpers

    private void warn(String path, String rule, Object got, Object def) {
        String message = "config.yml: " + path + " " + rule + ", got " + got + ". Using default " + def + ".";
        warnings.add(message);
        log.warning(message);
    }

    private int intAt(String path, int def, int min, int max) {
        Object raw = c.get(path);
        if (!(raw instanceof Number n)) {
            warn(path, "must be a whole number between " + min + " and " + max, raw, def);
            return def;
        }
        int v = n.intValue();
        if (v < min || v > max) {
            warn(path, "must be between " + min + " and " + max, v, def);
            return def;
        }
        return v;
    }

    private double dbl(String path, double def, double min, double max) {
        Object raw = c.get(path);
        if (!(raw instanceof Number n)) {
            warn(path, "must be a number between " + min + " and " + max, raw, def);
            return def;
        }
        double v = n.doubleValue();
        if (Double.isNaN(v) || v < min || v > max) {
            warn(path, "must be between " + min + " and " + max, v, def);
            return def;
        }
        return v;
    }

    private boolean bool(String path, boolean def) {
        Object raw = c.get(path);
        if (!(raw instanceof Boolean b)) {
            warn(path, "must be true or false", raw, def);
            return def;
        }
        return b;
    }

    private String str(String path, String def) {
        String v = c.getString(path);
        if (v == null || v.trim().isEmpty()) {
            warn(path, "must be a non-empty text value", v, def);
            return def;
        }
        return v.trim();
    }

    private <E extends Enum<E>> E enumAt(String path, Class<E> type, E def) {
        String v = c.getString(path);
        if (v == null) {
            warn(path, "must be one of " + java.util.Arrays.toString(type.getEnumConstants()), null, def);
            return def;
        }
        try {
            return Enum.valueOf(type, v.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            warn(path, "must be one of " + java.util.Arrays.toString(type.getEnumConstants()), v, def);
            return def;
        }
    }

    private Set<Integer> intSet(String path, List<Integer> def) {
        List<Integer> list = c.getIntegerList(path);
        if (list == null || list.isEmpty()) {
            warn(path, "must be a list of whole numbers", c.get(path), def);
            return new HashSet<>(def);
        }
        return new HashSet<>(list);
    }

    private Sound sound(String path, String defName) {
        Sound def;
        try {
            def = SoundUtil.parse(defName);
        } catch (IllegalArgumentException e) {
            def = null;
        }
        String v = c.getString(path);
        if (v == null) {
            warn(path, "must be a sound name or NONE", null, defName);
            return def;
        }
        try {
            return SoundUtil.parse(v);
        } catch (IllegalArgumentException e) {
            warn(path, "must be a valid 1.21 sound name (see the Sound list) or NONE", v, defName);
            return def;
        }
    }

    private Particle particle(String path, Particle def) {
        String v = c.getString(path);
        if (v == null) {
            warn(path, "must be a particle name", null, def);
            return def;
        }
        Particle p;
        try {
            p = Particle.valueOf(v.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            warn(path, "must be a valid particle name", v, def);
            return def;
        }
        if (p.getDataType() != Void.class) {
            warn(path, "must be a particle that needs no extra data (not DUST, BLOCK, ITEM...)", v, def);
            return def;
        }
        return p;
    }

    private ParticleSpec particles(String path, Particle defParticle, int defCount) {
        return new ParticleSpec(particle(path + ".particle", defParticle), intAt(path + ".count", defCount, 0, 500));
    }

    private BarSpec bar(String path, String defTitle, BarColor defColor, BarStyle defStyle) {
        String title = c.getString(path + ".title");
        if (title == null) {
            warn(path + ".title", "must be a text value", null, defTitle);
            title = defTitle;
        }
        return new BarSpec(title, enumAt(path + ".color", BarColor.class, defColor), enumAt(path + ".style", BarStyle.class, defStyle));
    }

    private Material material(String path, Material def, boolean mustBeBlock) {
        String v = c.getString(path);
        if (v == null) {
            warn(path, "must be a material name", null, def);
            return def;
        }
        Material m = Material.matchMaterial(v.trim());
        if (m == null || (mustBeBlock && !m.isBlock()) || (!mustBeBlock && !m.isItem())) {
            warn(path, mustBeBlock ? "must be a block material" : "must be an item material", v, def);
            return def;
        }
        return m;
    }

    private Vector vec3(String path, Vector def) {
        List<?> list = c.getList(path);
        Vector v = vec3(list);
        if (v == null) {
            warn(path, "must be a list of three numbers [x, y, z]", list, "[" + def.getX() + ", " + def.getY() + ", " + def.getZ() + "]");
            return def.clone();
        }
        return v;
    }

    private static Vector vec3(List<?> list) {
        if (list == null || list.size() != 3) {
            return null;
        }
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            if (!(list.get(i) instanceof Number n) || Double.isNaN(n.doubleValue())) {
                return null;
            }
            out[i] = n.doubleValue();
        }
        return new Vector(out[0], out[1], out[2]);
    }

    private RigSpec.ItemPart itemPart(String path, boolean defEnabled, Material defItem, float defScale, Vector defOffset, float defYaw) {
        boolean enabled = bool(path + ".enabled", defEnabled);
        Material item = material(path + ".item", defItem, false);
        float scale = (float) dbl(path + ".scale", defScale, 0.05, 50);
        Vector offset = vec3(path + ".offset", defOffset);
        float yaw = (float) dbl(path + ".yaw", defYaw, -360, 360);
        ItemDisplay.ItemDisplayTransform mode = enumAt(path + ".display-mode", ItemDisplay.ItemDisplayTransform.class, ItemDisplay.ItemDisplayTransform.FIXED);
        return new RigSpec.ItemPart(enabled, item, scale, offset, yaw, mode);
    }

    private RigSpec loadRig() {
        RigSpec.ItemPart cart = itemPart("bus.rig.item", true, Material.MINECART, 3.0f, new Vector(0, 0.5, 0), 90f);
        RigSpec.ItemPart banner = itemPart("bus.rig.banner", false, Material.WHITE_BANNER, 1.5f, new Vector(0, 3, -4), 0f);

        List<RigSpec.BlockPart> blocks = new ArrayList<>();
        List<Map<?, ?>> entries = c.getMapList("bus.rig.blocks");
        int index = 0;
        for (Map<?, ?> entry : entries) {
            String path = "bus.rig.blocks[" + index + "]";
            index++;
            Object blockName = entry.get("block");
            Material block = blockName == null ? null : Material.matchMaterial(String.valueOf(blockName));
            if (block == null || !block.isBlock()) {
                warn(path + ".block", "must be a block material", blockName, "skipping this entry");
                continue;
            }
            Vector offset = vec3(entry.get("offset") instanceof List<?> l ? l : null);
            Vector size = vec3(entry.get("size") instanceof List<?> l ? l : null);
            if (offset == null || size == null) {
                warn(path, "must have offset and size as lists of three numbers", entry, "skipping this entry");
                continue;
            }
            if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0 || size.length() > 200) {
                warn(path + ".size", "must have positive components (max 200)", size, "skipping this entry");
                continue;
            }
            blocks.add(new RigSpec.BlockPart(block, offset, size));
        }
        if (blocks.isEmpty()) {
            warn("bus.rig.blocks", "must contain at least one valid entry", entries.size() + " entries", "a plain 11x0.5x8 plank floor");
            blocks.add(new RigSpec.BlockPart(Material.DARK_OAK_PLANKS, new Vector(0, -0.5, 0), new Vector(11, 0.5, 8)));
        }
        return new RigSpec(cart, banner, blocks);
    }
}

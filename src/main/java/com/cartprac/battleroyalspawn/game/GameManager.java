package com.cartprac.battleroyalspawn.game;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.api.GameState;
import com.cartprac.battleroyalspawn.api.events.BusDepartEvent;
import com.cartprac.battleroyalspawn.api.events.GameResetEvent;
import com.cartprac.battleroyalspawn.api.events.GameStateChangeEvent;
import com.cartprac.battleroyalspawn.api.events.PlayerJumpFromBusEvent;
import com.cartprac.battleroyalspawn.api.events.PlayerLandEvent;
import com.cartprac.battleroyalspawn.bus.BusRig;
import com.cartprac.battleroyalspawn.bus.ChunkWindow;
import com.cartprac.battleroyalspawn.bus.FlightPath;
import com.cartprac.battleroyalspawn.bus.SeatManager;
import com.cartprac.battleroyalspawn.config.Messages;
import com.cartprac.battleroyalspawn.config.Settings;
import com.cartprac.battleroyalspawn.drop.GliderManager;
import com.cartprac.battleroyalspawn.drop.LandingDetector;
import com.cartprac.battleroyalspawn.game.PlayerSession.Phase;
import com.cartprac.battleroyalspawn.util.Tags;
import com.cartprac.battleroyalspawn.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The state machine: queue, countdown, cart flight, per-player drop and cleanup. Everything that touches
 * players or entities goes through here so there is exactly one place that knows the current state.
 */
public final class GameManager {

    private final CartDrop plugin;
    private final Logger log;
    private final HudManager hud;
    private final SeatManager seats;
    private final BusRig rig;
    private final ChunkWindow chunks;
    private final GliderManager glider;
    private final LandingDetector landing;
    private final Random random = new Random();

    private GameState state = GameState.IDLE;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final Map<UUID, PlayerSession> sessions = new LinkedHashMap<>();
    private final List<PlayerSession> scratch = new ArrayList<>();

    private int tick;
    private int countdownTicksLeft;
    private int countdownTotalTicks;
    private boolean countdownForced;
    private int lastCountdownSecond = -1;

    private FlightPath path;
    private World mapWorld;
    private int departTick;
    private int busTotal;
    private boolean doorsOpen;
    private boolean internalTeleport;

    private final LandingDetector.Callback landingCallback = new LandingDetector.Callback() {
        @Override
        public void landed(PlayerSession session, Player player, boolean safetyNet) {
            land(session, player, safetyNet);
        }

        @Override
        public void out(PlayerSession session, Player player, String reason) {
            removeFromMatch(session, player, reason, true);
        }
    };

    public GameManager(CartDrop plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.hud = new HudManager(plugin);
        this.seats = new SeatManager(plugin);
        this.rig = new BusRig(plugin);
        this.chunks = new ChunkWindow(plugin);
        this.glider = new GliderManager(plugin);
        this.landing = new LandingDetector(plugin);
    }

    // ------------------------------------------------------------------ accessors

    public GameState state() {
        return state;
    }

    public int currentTick() {
        return tick;
    }

    public boolean doorsOpen() {
        return doorsOpen;
    }

    public FlightPath path() {
        return path;
    }

    public World mapWorld() {
        return mapWorld;
    }

    public HudManager hud() {
        return hud;
    }

    public GliderManager glider() {
        return glider;
    }

    public BusRig rig() {
        return rig;
    }

    public ChunkWindow chunks() {
        return chunks;
    }

    public boolean isInternalTeleport() {
        return internalTeleport;
    }

    public Set<UUID> queueIds() {
        return Collections.unmodifiableSet(queue);
    }

    public Collection<PlayerSession> sessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    public PlayerSession session(UUID uuid) {
        return sessions.get(uuid);
    }

    public boolean isQueued(UUID uuid) {
        return queue.contains(uuid);
    }

    public boolean isGraced(UUID uuid) {
        PlayerSession s = sessions.get(uuid);
        return s != null && s.graceUntilTick > tick;
    }

    public boolean isInDrop(UUID uuid) {
        PlayerSession s = sessions.get(uuid);
        return s != null && s.phase == Phase.DROPPING;
    }

    public boolean isAboard(UUID uuid) {
        PlayerSession s = sessions.get(uuid);
        return s != null && s.isOnCart();
    }

    public boolean isManaged(UUID uuid) {
        PlayerSession s = sessions.get(uuid);
        return s != null && s.isManaged();
    }

    /** Online players currently in the queue. */
    public List<Player> queuedPlayers() {
        List<Player> out = new ArrayList<>(queue.size());
        for (UUID uuid : queue) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Online players in the current match. */
    public List<Player> participants() {
        List<Player> out = new ArrayList<>(sessions.size());
        for (PlayerSession s : sessions.values()) {
            Player p = s.player();
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ queue

    /** Adds the player to the queue (the clickable chat message runs this). */
    public void join(Player p) {
        Settings cfg = plugin.settings();
        Messages m = plugin.messages();
        UUID uuid = p.getUniqueId();
        if (sessions.containsKey(uuid)) {
            p.sendMessage(m.get("queue-in-match"));
            return;
        }
        if (!queue.add(uuid)) {
            p.sendMessage(m.get("queue-already"));
            return;
        }
        hud.sound(p, cfg.soundQueueJoin);
        if (state == GameState.IDLE || state == GameState.COUNTDOWN) {
            p.sendMessage(m.get("queue-joined", "queued", queue.size(), "min", cfg.minPlayers));
        } else {
            p.sendMessage(m.get("queue-joined-match-running", "queued", queue.size()));
        }
        p.spigot().sendMessage(Text.clickable(m.prefix(), m.plain("announce-leave-click"), "", "/br leave"));
        if (state == GameState.IDLE) {
            maybeStartCountdown();
        }
    }

    /**
     * Leaves the queue, or the match. A seated player jumps instead (if the doors are open); a gliding or
     * landed player is returned to their return location.
     *
     * @return false if the player was neither queued nor in a match
     */
    public boolean leave(Player p) {
        Messages m = plugin.messages();
        UUID uuid = p.getUniqueId();
        if (queue.remove(uuid)) {
            p.sendMessage(m.get("queue-left"));
            return true;
        }
        PlayerSession s = sessions.get(uuid);
        if (s == null) {
            p.sendMessage(m.get("not-in-queue"));
            return false;
        }
        if (s.isOnCart() && state == GameState.BUS) {
            if (!requestJump(p, s, false)) {
                // doors closed or a plugin refused the jump: take them out of the match instead
                removeFromMatch(s, p, "leave", true);
            }
            return true;
        }
        removeFromMatch(s, p, "leave", true);
        return true;
    }

    /** Sends the clickable queue message to every target that can and has not yet joined. Returns how many. */
    public int announce(Collection<? extends Player> targets) {
        Settings cfg = plugin.settings();
        Messages m = plugin.messages();
        int sent = 0;
        for (Player p : targets) {
            if (!p.hasPermission("cartdrop.play") || queue.contains(p.getUniqueId()) || sessions.containsKey(p.getUniqueId())) {
                continue;
            }
            p.spigot().sendMessage(Text.clickable(
                    m.get("announce", "queued", queue.size(), "min", cfg.minPlayers),
                    m.plain("announce-click"),
                    m.plain("announce-hover"),
                    "/br join"));
            sent++;
        }
        return sent;
    }

    private void pruneQueue() {
        queue.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    // ------------------------------------------------------------------ countdown

    private void maybeStartCountdown() {
        if (state == GameState.IDLE && queue.size() >= plugin.settings().minPlayers) {
            startCountdown(false);
        }
    }

    /**
     * Starts the countdown. With {@code force} the minimum player check is skipped, and a running
     * countdown is skipped to zero.
     *
     * @return true if the countdown is now running or was skipped
     */
    public boolean startCountdown(boolean force) {
        Settings cfg = plugin.settings();
        if (state == GameState.COUNTDOWN) {
            if (force) {
                countdownTicksLeft = 0;
                countdownForced = true;
            }
            return force;
        }
        if (state != GameState.IDLE) {
            return false;
        }
        pruneQueue();
        if (!force && queue.size() < cfg.minPlayers) {
            return false;
        }
        if (!setState(GameState.COUNTDOWN, true)) {
            return false;
        }
        countdownForced = force;
        int seconds = queue.size() >= cfg.maxPlayers ? cfg.countdownShortSeconds : cfg.countdownSeconds;
        countdownTotalTicks = seconds * 20;
        countdownTicksLeft = countdownTotalTicks;
        lastCountdownSecond = -1;
        for (Player p : queuedPlayers()) {
            p.sendMessage(plugin.messages().get("countdown-start", "seconds", seconds));
        }
        log.info("Countdown started: " + seconds + "s with " + queue.size() + " queued" + (force ? " (forced)" : ""));
        return true;
    }

    private void cancelCountdown() {
        hud.hide();
        for (Player p : queuedPlayers()) {
            p.sendMessage(plugin.messages().get("countdown-cancel"));
        }
        countdownTicksLeft = 0;
        countdownForced = false;
        setState(GameState.IDLE, false);
        log.info("Countdown cancelled: queue dropped below the minimum");
    }

    private void tickCountdown() {
        Settings cfg = plugin.settings();
        Messages m = plugin.messages();
        pruneQueue();
        if (!countdownForced && queue.size() < cfg.minPlayers) {
            cancelCountdown();
            return;
        }
        int shortTicks = cfg.countdownShortSeconds * 20;
        if (queue.size() >= cfg.maxPlayers && countdownTicksLeft > shortTicks) {
            countdownTicksLeft = shortTicks;
            countdownTotalTicks = shortTicks;
            for (Player p : queuedPlayers()) {
                p.sendMessage(m.get("countdown-shortened", "seconds", cfg.countdownShortSeconds));
            }
        }
        int seconds = (int) Math.ceil(countdownTicksLeft / 20.0);
        boolean secondChanged = seconds != lastCountdownSecond;
        if (secondChanged) {
            lastCountdownSecond = seconds;
            if (seconds > 0 && cfg.announceSeconds.contains(seconds)) {
                for (Player p : queuedPlayers()) {
                    p.sendMessage(m.get("countdown-tick", "seconds", seconds));
                    hud.title(p, m.plain("countdown-title", "seconds", seconds), m.plain("countdown-subtitle", "seconds", seconds));
                    hud.sound(p, seconds <= 3 ? cfg.soundCountdownFinal : cfg.soundCountdownTick);
                }
            }
        }
        if (secondChanged || tick % 5 == 0) {
            List<Player> queued = queuedPlayers();
            String title = Text.color(Text.fill(cfg.barCountdown.title, "seconds", seconds, "players", queued.size(),
                    "min", cfg.minPlayers, "max", cfg.maxPlayers));
            double progress = countdownTotalTicks <= 0 ? 0 : countdownTicksLeft / (double) countdownTotalTicks;
            hud.show(cfg.barCountdown, title, progress, queued);
        }
        if (countdownTicksLeft <= 0) {
            depart();
            return;
        }
        countdownTicksLeft--;
    }

    // ------------------------------------------------------------------ departure

    private void depart() {
        Settings cfg = plugin.settings();
        Messages m = plugin.messages();
        World world = Bukkit.getWorld(cfg.mapWorld);
        if (world == null) {
            log.severe("map.world '" + cfg.mapWorld + "' is not loaded; the match cannot start. Fix config.yml or load the world.");
            for (Player p : queuedPlayers()) {
                p.sendMessage(m.get("world-missing", "world", cfg.mapWorld));
            }
            hud.hide();
            countdownForced = false;
            setState(GameState.IDLE, false);
            return;
        }
        pruneQueue();
        List<Player> players = new ArrayList<>();
        for (Iterator<UUID> it = queue.iterator(); it.hasNext() && players.size() < cfg.maxPlayers; ) {
            Player p = Bukkit.getPlayer(it.next());
            it.remove();
            if (p != null) {
                players.add(p);
            }
        }
        if (players.isEmpty() || (!countdownForced && players.size() < cfg.minPlayers)) {
            for (Player p : players) {
                queue.add(p.getUniqueId());
            }
            cancelCountdown();
            return;
        }
        if (!setState(GameState.BUS, true)) {
            for (Player p : players) {
                queue.add(p.getUniqueId());
            }
            hud.hide();
            countdownForced = false;
            setState(GameState.IDLE, false);
            return;
        }

        mapWorld = world;
        path = FlightPath.build(cfg, world, random);
        departTick = tick;
        doorsOpen = false;
        busTotal = players.size();
        countdownForced = false;
        log.info("Bus departing with " + busTotal + " players: " + path.describe());

        Location origin = path.location();
        chunks.begin(world, path.position(), path.direction(), cfg.chunkAhead, cfg.chunkBehind);
        rig.spawn(origin);
        hud.hide();

        int index = 0;
        for (Player p : players) {
            PlayerSession s = new PlayerSession(p, index++, tick);
            s.returnLocation = p.getLocation().clone();
            sessions.put(p.getUniqueId(), s);
            Location target = seats.target(origin, s.seatIndex, busTotal);
            if (p.isInsideVehicle()) {
                p.leaveVehicle();
            }
            p.setFallDistance(0);
            p.setVelocity(new Vector(0, 0, 0));
            teleportInternal(p, target);
            p.sendMessage(m.get("bus-depart"));
            hud.sound(p, cfg.soundDepart);
        }
        Bukkit.getPluginManager().callEvent(new BusDepartEvent(path, players));
    }

    // ------------------------------------------------------------------ tick

    /** Called once per server tick by {@link GameLoop}. */
    public void tick() {
        tick++;
        switch (state) {
            case IDLE -> tickIdle();
            case COUNTDOWN -> tickCountdown();
            case BUS -> tickBus();
            case DROP -> tickDrop();
            default -> {
            }
        }
    }

    private void tickIdle() {
        if (tick % 20 != 0) {
            return;
        }
        Settings cfg = plugin.settings();
        pruneQueue();
        if (queue.size() >= cfg.minPlayers) {
            startCountdown(false);
            return;
        }
        if (cfg.announceIntervalSeconds > 0 && tick % (cfg.announceIntervalSeconds * 20) == 0) {
            announce(Bukkit.getOnlinePlayers());
        }
    }

    private boolean mapWorldGone() {
        return mapWorld == null || Bukkit.getWorld(mapWorld.getName()) == null;
    }

    private void tickBus() {
        if (tick % 20 == 0 && mapWorldGone()) {
            stop("world-unload", true);
            return;
        }
        Settings cfg = plugin.settings();
        Messages m = plugin.messages();

        path.advance();
        Location origin = path.location();
        rig.move(origin);
        if ((tick & 1) == 0) {
            rig.trail(origin, path.direction());
        }
        chunks.update(path.position(), path.direction());

        if (!doorsOpen && tick - departTick >= cfg.doorOpenAfterTicks()) {
            doorsOpen = true;
            for (PlayerSession s : sessions.values()) {
                Player p = s.player();
                if (p != null && s.isOnCart()) {
                    p.sendMessage(m.get("doors-open"));
                    hud.sound(p, cfg.soundDoorsOpen);
                }
            }
            log.info("Doors open");
        }

        boolean closeNow = path.fraction() >= cfg.doorCloseAt || path.isFinished();
        int aboard = 0;
        scratch.clear();
        scratch.addAll(sessions.values());
        for (PlayerSession s : scratch) {
            if (state != GameState.BUS) {
                break; // an event handler stopped the match mid-loop
            }
            Player p = s.player();
            if (p == null) {
                continue;
            }
            switch (s.phase) {
                case TRANSFERRING -> {
                    if (closeNow) {
                        requestJump(p, s, true);
                        break;
                    }
                    aboard++;
                    if (!p.getWorld().equals(mapWorld)) {
                        if (tick - s.transferTick > 100) {
                            if (!s.retriedTeleport) {
                                s.retriedTeleport = true;
                                teleportInternal(p, seats.target(origin, s.seatIndex, busTotal));
                            } else {
                                log.warning(s.name + " never arrived in the arena world; removing them from the match");
                                removeFromMatch(s, p, "transfer-failed", true);
                            }
                        }
                        break;
                    }
                    if (tick - s.transferTick < cfg.mountDelayTicks) {
                        break;
                    }
                    Location target = seats.target(origin, s.seatIndex, busTotal);
                    ArmorStand seat = seats.spawn(target);
                    if (seats.mount(seat, p)) {
                        s.seat = seat;
                        s.phase = Phase.ABOARD;
                        p.setFallDistance(0);
                    } else {
                        seat.remove();
                        if (tick - s.transferTick > 60) {
                            log.warning("Could not mount " + s.name + " on their seat; dropping them instead");
                            requestJump(p, s, true);
                        }
                    }
                }
                case ABOARD -> {
                    if (closeNow) {
                        requestJump(p, s, true);
                        break;
                    }
                    ArmorStand seat = s.seat;
                    if (seat == null || !seat.isValid() || !seat.equals(p.getVehicle())) {
                        log.info(s.name + " lost their seat; treating it as a jump");
                        requestJump(p, s, true);
                        break;
                    }
                    aboard++;
                    seats.move(seat, seats.target(origin, s.seatIndex, busTotal));
                }
                case DROPPING -> landing.tick(s, p, landingCallback);
                default -> {
                }
            }
        }
        if (state != GameState.BUS) {
            return;
        }
        if (closeNow) {
            log.info("Cart reached the door-close point; everyone still aboard was force-dropped");
            for (Player p : participants()) {
                p.sendMessage(m.get("forced-drop"));
                hud.sound(p, cfg.soundForcedDrop);
            }
            endBusPhase();
        } else if (aboard == 0) {
            endBusPhase();
        }
        if (state == GameState.BUS) {
            if (tick % 5 == 0) {
                updateBusHud(aboard);
            }
        } else if (state == GameState.DROP) {
            checkLive();
        }
    }

    private void endBusPhase() {
        rig.despawn();
        chunks.release();
        setState(GameState.DROP, false);
    }

    private void tickDrop() {
        if (tick % 20 == 0 && mapWorldGone()) {
            stop("world-unload", true);
            return;
        }
        scratch.clear();
        scratch.addAll(sessions.values());
        for (PlayerSession s : scratch) {
            if (state != GameState.DROP) {
                break;
            }
            Player p = s.player();
            if (p == null) {
                continue;
            }
            if (s.phase == Phase.DROPPING) {
                landing.tick(s, p, landingCallback);
            } else if (s.isOnCart()) {
                requestJump(p, s, true);
            }
        }
        if (state != GameState.DROP) {
            return;
        }
        if (tick % 5 == 0) {
            updateDropHud();
        }
        checkLive();
    }

    private void checkLive() {
        if (state != GameState.DROP) {
            return;
        }
        for (PlayerSession s : sessions.values()) {
            if (s.isManaged() && s.player() != null) {
                return;
            }
        }
        hud.hide();
        setState(GameState.LIVE, false);
        for (Player p : participants()) {
            p.sendMessage(plugin.messages().get("live"));
        }
        log.info("Everyone has landed: LIVE. CartDrop is no longer managing players.");
    }

    // ------------------------------------------------------------------ HUD

    private int secondsUntilDoors() {
        int left = plugin.settings().doorOpenAfterTicks() - (tick - departTick);
        return Math.max(0, (int) Math.ceil(left / 20.0));
    }

    private void updateBusHud(int aboard) {
        Settings cfg = plugin.settings();
        Messages m = plugin.messages();
        int seconds = (int) Math.ceil(path.ticksUntil(cfg.doorCloseAt) / 20.0);
        double closeDistance = cfg.doorCloseAt * path.getLength();
        double progress = closeDistance <= 0 ? 0 : 1.0 - path.getProgress() / closeDistance;
        String title = Text.color(Text.fill(cfg.barBus.title, "seconds", seconds, "aboard", aboard, "total", busTotal));
        hud.show(cfg.barBus, title, progress, participants());
        for (PlayerSession s : scratch) {
            Player p = s.player();
            if (p == null) {
                continue;
            }
            if (s.isOnCart()) {
                hud.actionBar(p, doorsOpen
                        ? m.plain("aboard-actionbar", "aboard", aboard, "total", busTotal)
                        : m.plain("doors-closed-actionbar", "seconds", secondsUntilDoors()));
            } else if (s.phase == Phase.DROPPING) {
                dropActionBar(p, s);
            }
        }
    }

    private void updateDropHud() {
        Settings cfg = plugin.settings();
        int landed = 0;
        int total = 0;
        for (PlayerSession s : sessions.values()) {
            if (s.phase == Phase.LANDED) {
                landed++;
            }
            if (s.phase != Phase.OUT) {
                total++;
            }
        }
        String title = Text.color(Text.fill(cfg.barDrop.title, "landed", landed, "total", total));
        hud.show(cfg.barDrop, title, total <= 0 ? 1.0 : landed / (double) total, participants());
        for (PlayerSession s : scratch) {
            Player p = s.player();
            if (p != null && s.phase == Phase.DROPPING) {
                dropActionBar(p, s);
            }
        }
    }

    private void dropActionBar(Player p, PlayerSession s) {
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w == null) {
            return;
        }
        int ground = w.getHighestBlockYAt(l.getBlockX(), l.getBlockZ());
        int altitude = Math.max(0, (int) (l.getY() - ground - 1));
        int distance = 0;
        if (s.jumpLocation != null && s.jumpLocation.isWorldLoaded() && w.equals(s.jumpLocation.getWorld())) {
            distance = (int) Math.hypot(l.getX() - s.jumpLocation.getX(), l.getZ() - s.jumpLocation.getZ());
        }
        hud.actionBar(p, plugin.messages().plain("drop-actionbar", "altitude", altitude, "distance", distance));
    }

    // ------------------------------------------------------------------ jump

    /** Ejects a seated player. Forced jumps ignore closed doors and event cancellation. */
    public boolean requestJump(Player p, boolean forced) {
        PlayerSession s = sessions.get(p.getUniqueId());
        return s != null && requestJump(p, s, forced);
    }

    private boolean requestJump(Player p, PlayerSession s, boolean forced) {
        if (!s.isOnCart() || state != GameState.BUS) {
            return false;
        }
        Settings cfg = plugin.settings();
        Messages m = plugin.messages();
        if (!forced && !doorsOpen) {
            hud.actionBar(p, m.plain("doors-closed-actionbar", "seconds", secondsUntilDoors()));
            return false;
        }
        Location loc = p.getLocation();
        PlayerJumpFromBusEvent event = new PlayerJumpFromBusEvent(p, loc, forced);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() && !forced) {
            return false;
        }
        // Phase changes first: removing the seat fires a dismount event, and that handler must see DROPPING.
        s.phase = Phase.DROPPING;
        seats.remove(s);
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        glider.equip(p, s);
        Vector dir = path != null ? path.direction() : loc.getDirection().setY(0);
        if (dir.lengthSquared() < 1.0e-6) {
            dir = new Vector(0, 0, 1);
        }
        dir.normalize();
        p.setFallDistance(0);
        p.setVelocity(dir.multiply(cfg.jumpForward).setY(-cfg.jumpDown));
        p.setGliding(true);
        s.jumpLocation = loc.clone();
        s.airtimeTicks = 0;
        s.groundTicks = 0;
        s.gliderStarted = false;
        hud.title(p, m.plain("jump-title"), m.plain("jump-subtitle"));
        hud.sound(p, cfg.soundJump);
        hud.particles(loc, cfg.jumpParticles);
        return true;
    }

    /** Forces every seated player off the cart. Returns how many jumped. */
    public int forceJumpAll() {
        int count = 0;
        scratch.clear();
        scratch.addAll(sessions.values());
        for (PlayerSession s : scratch) {
            Player p = s.player();
            if (p != null && s.isOnCart() && requestJump(p, s, true)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Called by the dismount listener whenever a player is about to leave a seat entity.
     *
     * @return true to cancel the dismount and keep the player seated
     */
    public boolean onDismountAttempt(Player p) {
        PlayerSession s = sessions.get(p.getUniqueId());
        if (s == null || s.phase != Phase.ABOARD || state != GameState.BUS) {
            return false; // not ours, or already handled (phase is DROPPING/OUT)
        }
        Settings cfg = plugin.settings();
        if (!doorsOpen) {
            hud.actionBar(p, plugin.messages().plain("doors-closed-actionbar", "seconds", secondsUntilDoors()));
            return true;
        }
        if (cfg.jumpKey == Settings.JumpKey.RIGHT_CLICK && p.isSneaking()) {
            return true; // sneaking is not the jump key on this server
        }
        return !requestJump(p, s, false);
    }

    // ------------------------------------------------------------------ landing

    void land(PlayerSession s, Player p, boolean safetyNet) {
        if (s.phase != Phase.DROPPING) {
            return;
        }
        Settings cfg = plugin.settings();
        Messages m = plugin.messages();
        s.phase = Phase.LANDED;
        p.setGliding(false);
        glider.restore(p, s);
        p.setFallDistance(0);
        if (cfg.graceSeconds > 0) {
            s.graceUntilTick = tick + cfg.graceSeconds * 20;
        }
        hud.sound(p, cfg.soundLanding);
        hud.particles(p.getLocation(), cfg.landingParticles);
        hud.title(p, m.plain("landed-title", "grace", cfg.graceSeconds), m.plain("landed-subtitle", "grace", cfg.graceSeconds));
        if (safetyNet) {
            p.sendMessage(m.get("safety-landed"));
        }
        Bukkit.getPluginManager().callEvent(new PlayerLandEvent(p, p.getLocation(), s.airtimeTicks, safetyNet));
    }

    // ------------------------------------------------------------------ leaving the match

    private void markOut(PlayerSession s, Player p, String reason) {
        seats.remove(s);
        if (p != null) {
            if (s.gliderEquipped) {
                glider.restore(p, s);
            }
            p.setGliding(false);
            p.setFallDistance(0);
            if (p.isInsideVehicle()) {
                p.leaveVehicle();
            }
        }
        s.phase = Phase.OUT;
        sessions.remove(s.uuid);
        log.info(s.name + " is out of the match (" + reason + ")");
    }

    private void removeFromMatch(PlayerSession s, Player p, String reason, boolean sendBack) {
        markOut(s, p, reason);
        if (p != null && sendBack) {
            teleportInternal(p, returnLocationFor(s));
            p.sendMessage(plugin.messages().get("left-match"));
        }
    }

    /** Player disconnected: clean up now, send them back when they next log in. */
    public void handleQuit(Player p) {
        UUID uuid = p.getUniqueId();
        queue.remove(uuid);
        PlayerSession s = sessions.get(uuid);
        if (s == null) {
            return;
        }
        boolean wasManaged = s.isManaged();
        markOut(s, p, "quit");
        if (wasManaged) {
            plugin.returns().put(uuid, returnLocationFor(s));
        }
    }

    /** Player logged in: return them if they disconnected mid-match. */
    public void handleJoin(Player p) {
        if (plugin.returns().has(p.getUniqueId())) {
            Location loc = plugin.returns().take(p.getUniqueId());
            if (loc != null) {
                teleportInternal(p, loc);
                p.sendMessage(plugin.messages().get("returned"));
            }
        }
    }

    /** Player died mid-drop: keep the glider out of the drops and put the real chest item there instead. */
    public void handleDeath(Player p, PlayerDeathEvent event) {
        PlayerSession s = sessions.get(p.getUniqueId());
        if (s == null || !s.isManaged()) {
            return;
        }
        seats.remove(s);
        if (s.gliderEquipped) {
            event.getDrops().removeIf(glider::isGlider);
            if (event.getKeepInventory()) {
                glider.restore(p, s);
            } else {
                glider.stripAny(p);
                if (s.savedChest != null) {
                    event.getDrops().add(s.savedChest);
                }
                s.savedChest = null;
                s.gliderEquipped = false;
            }
        }
        s.phase = Phase.OUT;
        sessions.remove(s.uuid);
        log.info(s.name + " died mid-drop");
    }

    /** Another plugin teleported a managed player. */
    public void handleTeleport(Player p, PlayerTeleportEvent event) {
        if (internalTeleport) {
            return;
        }
        PlayerSession s = sessions.get(p.getUniqueId());
        if (s == null || !s.isManaged()) {
            return;
        }
        World to = event.getTo() == null ? null : event.getTo().getWorld();
        boolean leavingWorld = to == null || mapWorld == null || !to.equals(mapWorld);
        if (s.isOnCart()) {
            if (leavingWorld) {
                markOut(s, p, "teleported out of the arena");
            } else {
                requestJump(p, s, true);
            }
        } else if (leavingWorld) {
            markOut(s, p, "teleported out of the arena");
        }
    }

    /** Teleports without triggering our own teleport handling. */
    public void teleportInternal(Player p, Location loc) {
        if (loc == null) {
            return;
        }
        internalTeleport = true;
        try {
            p.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } finally {
            internalTeleport = false;
        }
    }

    private Location returnLocationFor(PlayerSession s) {
        Settings cfg = plugin.settings();
        Location loc = null;
        if (cfg.returnMode == Settings.ReturnMode.PREVIOUS && s.returnLocation != null && s.returnLocation.isWorldLoaded()) {
            loc = s.returnLocation;
        }
        if (loc == null) {
            loc = cfg.configuredReturnLocation();
        }
        if (loc == null) {
            loc = Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        return loc;
    }

    // ------------------------------------------------------------------ state, stop, reset

    private boolean setState(GameState next, boolean honourCancel) {
        if (next == state) {
            return true;
        }
        GameStateChangeEvent event = new GameStateChangeEvent(state, next);
        Bukkit.getPluginManager().callEvent(event);
        if (honourCancel && event.isCancelled()) {
            log.info("State change " + state + " -> " + next + " was cancelled by another plugin");
            return false;
        }
        log.info("State " + state + " -> " + next);
        state = next;
        return true;
    }

    /**
     * Ends whatever is running: removes cart entities and chunk tickets, restores gliders, optionally returns
     * players, and goes back to IDLE. The queue is kept.
     *
     * @return false if nothing was running
     */
    public boolean stop(String reason, boolean returnPlayers) {
        if (state == GameState.IDLE) {
            return false;
        }
        Messages m = plugin.messages();
        setState(GameState.RESETTING, false);
        hud.hide();
        scratch.clear();
        scratch.addAll(sessions.values());
        for (PlayerSession s : scratch) {
            Player p = s.player();
            seats.remove(s);
            if (p != null) {
                if (s.gliderEquipped) {
                    glider.restore(p, s);
                }
                p.setGliding(false);
                p.setFallDistance(0);
                if (p.isInsideVehicle()) {
                    p.leaveVehicle();
                }
                if (returnPlayers) {
                    teleportInternal(p, returnLocationFor(s));
                }
                p.sendMessage(m.get("stopped"));
            }
        }
        sessions.clear();
        rig.despawn();
        chunks.release();
        path = null;
        mapWorld = null;
        doorsOpen = false;
        countdownTicksLeft = 0;
        countdownForced = false;
        setState(GameState.IDLE, false);
        Bukkit.getPluginManager().callEvent(new GameResetEvent(reason));
        log.info("Match reset (" + reason + ")");
        return true;
    }

    /**
     * Leaves the server exactly as CartDrop found it: stops any match, then sweeps every world for tagged
     * entities and chunk tickets, strips leftover gliders from online players and drops the boss bar.
     * Used on enable (orphans from a crash), on disable and on {@code /reload}.
     */
    public void hardReset(String reason) {
        try {
            stop(reason, true);
        } catch (RuntimeException ex) {
            log.log(Level.SEVERE, "Error while stopping the match during " + reason, ex);
        }
        queue.clear();
        sessions.clear();
        for (World w : Bukkit.getWorlds()) {
            int removed = 0;
            for (Entity e : w.getEntities()) {
                if (Tags.isOurs(e)) {
                    e.remove();
                    removed++;
                }
            }
            w.removePluginChunkTickets(plugin);
            if (removed > 0) {
                log.info("Removed " + removed + " orphaned CartDrop entities from " + w.getName());
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (glider.stripAny(p) > 0) {
                p.setGliding(false);
                log.warning("Removed a leftover glider from " + p.getName() + " (their original chest item could not be restored)");
            }
        }
        hud.destroy();
        rig.despawn();
        chunks.release();
        path = null;
        mapWorld = null;
        doorsOpen = false;
        state = GameState.IDLE;
    }

    /** Lines for {@code /br state}. */
    public List<String> debugReport() {
        List<String> out = new ArrayList<>();
        out.add("state=" + state + " tick=" + tick + " doorsOpen=" + doorsOpen);
        out.add("queue=" + queue.size() + " sessions=" + sessions.size());
        int aboard = 0;
        int dropping = 0;
        int landed = 0;
        int seatEntities = 0;
        for (PlayerSession s : sessions.values()) {
            switch (s.phase) {
                case TRANSFERRING, ABOARD -> aboard++;
                case DROPPING -> dropping++;
                case LANDED -> landed++;
                default -> {
                }
            }
            if (s.seat != null && s.seat.isValid()) {
                seatEntities++;
            }
        }
        out.add("aboard=" + aboard + " dropping=" + dropping + " landed=" + landed);
        out.add("seatEntities=" + seatEntities + " rigParts=" + rig.partCount() + " chunkTickets=" + chunks.heldCount());
        out.add("pendingReturns=" + plugin.returns().size());
        if (path != null) {
            out.add("path=" + path.describe());
            out.add(String.format(java.util.Locale.ROOT, "progress=%.1f%%", path.fraction() * 100.0));
        }
        if (state == GameState.COUNTDOWN) {
            out.add("countdownTicksLeft=" + countdownTicksLeft + (countdownForced ? " (forced)" : ""));
        }
        for (PlayerSession s : sessions.values()) {
            out.add("  " + s.name + ": " + s.phase + (s.gliderEquipped ? " glider" : "") + " air=" + s.airtimeTicks
                    + (s.graceUntilTick > tick ? " grace=" + (s.graceUntilTick - tick) : ""));
        }
        return out;
    }
}

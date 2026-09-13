package com.cartprac.battleroyalspawn.game;

import com.cartprac.battleroyalspawn.CartDrop;
import com.cartprac.battleroyalspawn.config.Settings;
import com.cartprac.battleroyalspawn.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One shared boss bar (re-styled per phase), plus action bar, title, sound and particle helpers.
 */
public final class HudManager {

    private final CartDrop plugin;
    private BossBar bar;
    private final Set<UUID> shown = new HashSet<>();
    private final Set<UUID> wanted = new HashSet<>();

    public HudManager(CartDrop plugin) {
        this.plugin = plugin;
    }

    /** Shows (or updates) the boss bar for exactly the given players. */
    public void show(Settings.BarSpec spec, String titleColored, double progress, Collection<? extends Player> players) {
        if (bar == null) {
            bar = Bukkit.createBossBar("", spec.color, spec.style);
        }
        bar.setColor(spec.color);
        bar.setStyle(spec.style);
        bar.setTitle(titleColored);
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        bar.setVisible(true);

        wanted.clear();
        for (Player p : players) {
            wanted.add(p.getUniqueId());
            if (shown.add(p.getUniqueId())) {
                bar.addPlayer(p);
            }
        }
        shown.removeIf(uuid -> {
            if (wanted.contains(uuid)) {
                return false;
            }
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                bar.removePlayer(p);
            }
            return true;
        });
    }

    /** Hides the bar from everyone. */
    public void hide() {
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
        shown.clear();
        wanted.clear();
    }

    /** Hides and forgets the bar entirely (plugin disable). */
    public void destroy() {
        hide();
        bar = null;
    }

    public void title(Player p, String titleColored, String subtitleColored) {
        Settings s = plugin.settings();
        p.sendTitle(titleColored, subtitleColored, s.titleFadeIn, s.titleStay, s.titleFadeOut);
    }

    public void actionBar(Player p, String colored) {
        if (plugin.settings().actionBar) {
            Text.actionBar(p, colored);
        }
    }

    public void sound(Player p, Sound sound) {
        sound(p, sound, 1f);
    }

    public void sound(Player p, Sound sound, float pitch) {
        if (sound != null) {
            p.playSound(p.getLocation(), sound, 1f, pitch);
        }
    }

    public void particles(Location at, Settings.ParticleSpec spec) {
        World world = at.getWorld();
        if (spec.count > 0 && world != null) {
            world.spawnParticle(spec.particle, at, spec.count, 0.5, 0.5, 0.5, 0.05);
        }
    }
}

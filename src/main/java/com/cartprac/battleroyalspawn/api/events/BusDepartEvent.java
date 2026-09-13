package com.cartprac.battleroyalspawn.api.events;

import com.cartprac.battleroyalspawn.bus.FlightPath;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Fired once the participants have been moved to the arena world and the cart starts flying.
 */
public class BusDepartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final FlightPath path;
    private final List<Player> participants;

    public BusDepartEvent(FlightPath path, Collection<Player> participants) {
        this.path = path;
        this.participants = List.copyOf(participants);
    }

    /** The line the cart flies this match. */
    public FlightPath getPath() {
        return path;
    }

    /** Players on the cart, in seat order. */
    public List<Player> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

package com.cartprac.battleroyalspawn.util;

import org.bukkit.entity.Entity;

/**
 * Scoreboard tags stamped on every entity CartDrop spawns, so orphans can be found and removed.
 */
public final class Tags {

    /** Tag on every display entity that forms the cart. */
    public static final String BUS = "cartdrop_bus";
    /** Tag on every invisible seat armor stand. */
    public static final String SEAT = "cartdrop_seat";

    private Tags() {
    }

    /** True if the entity carries any CartDrop tag. */
    public static boolean isOurs(Entity entity) {
        if (entity == null) {
            return false;
        }
        var tags = entity.getScoreboardTags();
        return tags.contains(BUS) || tags.contains(SEAT);
    }
}

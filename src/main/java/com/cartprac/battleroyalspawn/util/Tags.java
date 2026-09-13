package com.cartprac.battleroyalspawn.util;

import org.bukkit.entity.Entity;

// scoreboard tags on everything we spawn so leftovers can be found after a crash
public final class Tags {

    public static final String BUS = "cartdrop_bus";
    public static final String SEAT = "cartdrop_seat";

    private Tags() {
    }

    public static boolean isOurs(Entity entity) {
        if (entity == null) {
            return false;
        }
        var tags = entity.getScoreboardTags();
        return tags.contains(BUS) || tags.contains(SEAT);
    }
}

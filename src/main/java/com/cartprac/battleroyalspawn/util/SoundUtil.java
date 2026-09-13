package com.cartprac.battleroyalspawn.util;

import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

// accepts BLOCK_IRON_DOOR_OPEN, block.iron_door.open, minecraft:block.iron_door.open or NONE
// Sound.valueOf is deprecated now so the constant names get matched against the registry keys instead
public final class SoundUtil {

    private SoundUtil() {
    }

    // null = NONE, throws if it doesn't exist
    public static Sound parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("missing sound name");
        }
        String s = raw.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("NONE")) {
            return null;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.contains(".") || lower.contains(":")) {
            try {
                Sound byKey = Registry.SOUNDS.match(lower);
                if (byKey != null) {
                    return byKey;
                }
            } catch (RuntimeException ignored) {
                // invalid key characters; fall through to the constant-name scan
            }
        }
        String constant = s.toUpperCase(Locale.ROOT);
        for (Sound sound : Registry.SOUNDS) {
            if (sound.getKeyOrThrow().getKey().replace('.', '_').toUpperCase(Locale.ROOT).equals(constant)) {
                return sound;
            }
        }
        throw new IllegalArgumentException("unknown sound '" + raw + "' for this server version");
    }
}

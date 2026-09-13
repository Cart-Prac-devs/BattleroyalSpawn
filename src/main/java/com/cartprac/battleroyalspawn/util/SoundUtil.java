package com.cartprac.battleroyalspawn.util;

import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

/**
 * Parses sound names from config. Accepts the classic constant names (BLOCK_IRON_DOOR_OPEN),
 * namespaced keys (block.iron_door.open or minecraft:block.iron_door.open) and NONE.
 * <p>
 * {@code Sound.valueOf} is deprecated in 1.21, so constant names are resolved by walking the sound
 * registry: a constant name is exactly the key with dots replaced by underscores, upper-cased.
 */
public final class SoundUtil {

    private SoundUtil() {
    }

    /**
     * @return the sound, or {@code null} when the value is NONE/empty
     * @throws IllegalArgumentException when the name matches nothing in this server version
     */
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

package com.cartprac.battleroyalspawn.config;

import com.cartprac.battleroyalspawn.util.Text;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

// messages: section. missing keys fall back to the jar defaults, bukkit merges those for us
public final class Messages {

    private final FileConfiguration config;
    private final Logger log;
    private final Set<String> warned = new HashSet<>();
    private final String prefix;

    public Messages(FileConfiguration config, Logger log) {
        this.config = config;
        this.log = log;
        this.prefix = Text.color(raw("prefix"));
    }

    public String raw(String key) {
        String value = config.getString("messages." + key);
        if (value == null) {
            if (warned.add(key)) {
                log.warning("config.yml: messages." + key + " is missing. Using the key name as text.");
            }
            return "&c<" + key + ">";
        }
        return value;
    }

    public String get(String key, Object... kv) {
        return prefix + Text.color(Text.fill(raw(key), kv));
    }

    public String plain(String key, Object... kv) {
        return Text.color(Text.fill(raw(key), kv));
    }

    public String prefix() {
        return prefix;
    }
}

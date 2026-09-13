package com.cartprac.battleroyalspawn.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class Text {

    private Text() {
    }

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String strip(String s) {
        return s == null ? "" : ChatColor.stripColor(s);
    }

    // fill("{a} {b}", "a", 1, "b", 2)
    public static String fill(String template, Object... kv) {
        if (template == null) {
            return "";
        }
        String out = template;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out = out.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return out;
    }

    public static void actionBar(Player player, String colored) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(colored));
    }

    public static BaseComponent[] clickable(String textColored, String clickColored, String hoverColored, String command) {
        BaseComponent head = TextComponent.fromLegacy(textColored);
        TextComponent click = new TextComponent(TextComponent.fromLegacy(clickColored));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        if (hoverColored != null && !hoverColored.isEmpty()) {
            click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new net.md_5.bungee.api.chat.hover.content.Text(hoverColored)));
        }
        return new BaseComponent[] {head, click};
    }
}

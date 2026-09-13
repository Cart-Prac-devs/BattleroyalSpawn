package com.cartprac.battleroyalspawn;

import com.cartprac.battleroyalspawn.api.CartDropAPI;
import com.cartprac.battleroyalspawn.commands.BrCommand;
import com.cartprac.battleroyalspawn.config.Messages;
import com.cartprac.battleroyalspawn.config.Settings;
import com.cartprac.battleroyalspawn.game.GameLoop;
import com.cartprac.battleroyalspawn.game.GameManager;
import com.cartprac.battleroyalspawn.game.ReturnStore;
import com.cartprac.battleroyalspawn.listeners.ConnectionListener;
import com.cartprac.battleroyalspawn.listeners.DamageListener;
import com.cartprac.battleroyalspawn.listeners.DismountListener;
import com.cartprac.battleroyalspawn.listeners.GliderListener;
import com.cartprac.battleroyalspawn.listeners.JumpListener;
import com.cartprac.battleroyalspawn.listeners.WorldListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CartDrop extends JavaPlugin {

    private static CartDrop instance;

    private Settings settings;
    private Messages messages;
    private ReturnStore returns;
    private GameManager game;
    private GameLoop loop;

    public static CartDrop get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        applyConfig();

        returns = new ReturnStore(this);
        returns.load();

        game = new GameManager(this);
        game.hardReset("enable"); // clean up leftovers from a crash / reload

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new JumpListener(this), this);
        pm.registerEvents(new DismountListener(this), this);
        pm.registerEvents(new ConnectionListener(this), this);
        pm.registerEvents(new DamageListener(this), this);
        pm.registerEvents(new GliderListener(this), this);
        pm.registerEvents(new WorldListener(this), this);

        PluginCommand command = getCommand("battleroyale");
        if (command != null) {
            BrCommand executor = new BrCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("Command 'battleroyale' is missing from plugin.yml");
        }

        loop = new GameLoop(this, game);
        loop.runTaskTimer(this, 1L, 1L);
        CartDropAPI.init(game);

        getLogger().info("CartDrop enabled. " + settings.warnings().size() + " config warning(s). Arena world: " + settings.mapWorld);
    }

    @Override
    public void onDisable() {
        CartDropAPI.shutdown();
        if (loop != null) {
            loop.cancel();
            loop = null;
        }
        if (game != null) {
            game.hardReset("disable");
        }
        if (returns != null) {
            returns.save();
        }
        instance = null;
    }

    public void applyConfig() {
        settings = new Settings(getConfig(), getLogger());
        messages = new Messages(getConfig(), getLogger());
    }

    public int reloadFromDisk() {
        reloadConfig();
        applyConfig();
        return settings.warnings().size();
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public ReturnStore returns() {
        return returns;
    }

    public GameManager game() {
        return game;
    }
}

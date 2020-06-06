package com.github.nova_27.mcplugin.discordconnect.utils;

import com.tjplaysnow.discord.object.ProgramThread;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class ThreadBungee extends ProgramThread {
    private Plugin plugin;

    public ThreadBungee(Plugin plugin) {
        super(false);
        this.plugin = plugin;
    }

    public void addAction(Runnable action, int seconds) {
        plugin.getProxy().getScheduler().schedule(this.plugin, action, (seconds * 20), TimeUnit.SECONDS);
    }

    public void stop() {
    }

    public void run() {
    }
}

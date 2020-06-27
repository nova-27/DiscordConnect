package com.github.nova_27.mcplugin.discordconnect.listener;

import com.github.nova_27.mcplugin.discordconnect.DiscordConnect;
import com.github.nova_27.mcplugin.discordconnect.utils.Messages;
import com.github.nova_27.mcplugin.servermanager.core.config.ConfigData;
import com.github.nova_27.mcplugin.servermanager.core.events.ServerEvent;
import com.github.nova_27.mcplugin.servermanager.core.events.TimerEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.awt.*;

public class SMFBListener implements Listener {
    /**
     * (SMFB)サーバーに関するイベントが起こったら
     * @param e サーバー情報
     */
    @EventHandler
    public void onServerEventHappen(ServerEvent e) {
        Color color = null;
        String mes = null;

        switch(e.getEventType()) {
            case ServerEnabled:
                color = Color.GREEN;
                mes = Messages.ServerEnabled.toString().replace("{server}", e.getServer().Name);
                break;
            case ServerDisabled:
                color = Color.RED;
                mes = Messages.ServerDisabled.toString().replace("{server}", e.getServer().Name);
                break;
            case ServerStarting:
                color = Color.YELLOW;
                mes = Messages.ServerStarting.toString().replace("{server}", e.getServer().Name);
                break;
            case ServerStopping:
                color = Color.YELLOW;
                mes = Messages.ServerStopping.toString().replace("{server}", e.getServer().Name);
                break;
            case ServerStarted:
                color = Color.GREEN;
                mes = Messages.ServerStarted.toString().replace("{server}", e.getServer().Name);
                break;
            case ServerStopped:
                color = Color.RED;
                mes = Messages.ServerStopped.toString().replace("{server}", e.getServer().Name);
                break;
            case ServerErrorHappened:
                color = Color.RED;
                mes = Messages.ServerError.toString().replace("{server}", e.getServer().Name);
                break;
        }

        DiscordConnect.getInstance().embed(color, mes, null);
    }

    /**
     * (SMFB)タイマーに関するイベントが起こったら
     * @param e サーバー情報
     */
    @EventHandler
    public void onTimerEventHappen(TimerEvent e) {
        Color color = null;
        String mes = null;

        switch(e.getEventType()) {
            case TimerStarted:
                color = Color.ORANGE;
                mes = Messages.TimerStarted.toString().replace("{server}", e.getServer().Name).replace("{time}", java.lang.String.valueOf(ConfigData.CloseTime));
                break;
            case TimerStopped:
                color = Color.ORANGE;
                mes = Messages.TimerStopped.toString().replace("{server}", e.getServer().Name);
                break;
            case TimerRestarted:
                color = Color.YELLOW;
                mes = Messages.TimerRestarted.toString().replace("{server}", e.getServer().Name);
                break;
        }

        DiscordConnect.getInstance().embed(color, mes, null);
    }
}

package com.github.nova_27.mcplugin.discordconnect.listener;

import com.github.nova_27.mcplugin.discordconnect.DiscordConnect;
import com.github.nova_27.mcplugin.discordconnect.utils.DiscordSender;
import com.github.nova_27.mcplugin.discordconnect.utils.Messages;
import com.github.nova_27.mcplugin.servermanager.core.config.ConfigData;
import com.github.nova_27.mcplugin.servermanager.core.events.ServerEvent;
import com.github.nova_27.mcplugin.servermanager.core.events.TimerEvent;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import com.mojang.brigadier.Message;
import net.dv8tion.jda.core.entities.Game;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * BungeeCordイベントリスナー
 */
public class BungeeEvent implements Listener {
    /**
     * チャットが送信されたら実行
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(ChatEvent event) {
        //コマンドなら
        if(event.isCommand()){
            return;
        }

        //キャンセルされていたら
        if (event.isCancelled()) return;

        // プレイヤーの発言ではない場合は、そのまま無視する
        if ( !(event.getSender() instanceof ProxiedPlayer) ) {
            return;
        }

        N8ChatCasterAPI chatCasterApi = DiscordConnect.getInstance().getChatCasterApi();
        if (chatCasterApi == null || !chatCasterApi.isEnabledChatCaster()) {
            // 連携プラグインが無効の場合
            ProxiedPlayer sender = (ProxiedPlayer)event.getSender();
            String senderServer = sender.getServer().getInfo().getName();
            String message = event.getMessage();

            MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
            String output = MarkdownConverter.toDiscordMessage(components);
            DiscordConnect.getInstance().mainChannel_AddQueue(
                    com.github.nova_27.mcplugin.discordconnect.ConfigData.toDiscord.toString()
                    .replace("{server}", senderServer)
                    .replace("{sender}", sender.getName())
                    .replace("{message}", output)
            );
        }
    }

    /**
     * ログインされたら
     * @param e ログイン情報
     */
    @EventHandler
    public void onLogin(LoginEvent e) {
        String name = e.getConnection().getName();
        DiscordConnect.getInstance().embed(Color.BLUE, Messages.joined.toString().replace("{name}", name), null);

        updatePlayerCount();
    }

    /**
     * 切断されたら
     * @param e 切断情報
     */
    @EventHandler
    public void onLogout(PlayerDisconnectEvent e) {
        String name = e.getPlayer().getName();
        DiscordConnect.getInstance().embed(Color.BLUE, Messages.left.toString().replace("{name}", name), null);

        updatePlayerCount();
    }

    /**
     * サーバー間を移動したら
     * @param e プレイヤー情報
     */
    @EventHandler
    public void onSwitch(ServerSwitchEvent e) {
        String name = e.getPlayer().getName();
        DiscordConnect.getInstance().embed(Color.CYAN, Messages.serverSwitched.toString().replace("{name}", name).replace("{server}", e.getPlayer().getServer().getInfo().getName()), null);
    }

    /**
     * プレイヤー数情報を更新
     */
    private void updatePlayerCount() {
        DiscordConnect.getInstance().getProxy().getScheduler().schedule(DiscordConnect.getInstance(), () -> {
            int maxPlayerInt = DiscordConnect.getInstance().getProxy().getConfig().getPlayerLimit();
            String maxPlayer = maxPlayerInt != -1 ? String.valueOf(maxPlayerInt) : "∞";

            DiscordConnect.getInstance().setGame(com.github.nova_27.mcplugin.discordconnect.ConfigData.playingGame
                    .replace("{players}", String.valueOf(DiscordConnect.getInstance().getProxy().getPlayers().size()))
                    .replace("{max}", maxPlayer)
            );
        },1L, TimeUnit.SECONDS);
    }
}

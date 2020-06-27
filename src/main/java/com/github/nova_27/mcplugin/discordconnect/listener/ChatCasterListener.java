package com.github.nova_27.mcplugin.discordconnect.listener;

import com.github.nova_27.mcplugin.discordconnect.DiscordConnect;
import com.github.nova_27.mcplugin.servermanager.core.Smfb_core;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.events.GlobalChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * 連携プラグイン用リスナー
 */
public class ChatCasterListener implements Listener {
    /**
     * グローバルチャットに送信されたら実行（連携プラグイン有効時のみ実行される）
     * @param event チャット情報
     */
    @EventHandler
    public void onGlobalChat(GlobalChatEvent event) {
        if (event.isCancelled()) return;

        String message = DiscordConnect.getInstance().getChatCasterApi().formatMessageForDiscord(event);
        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
        String output = MarkdownConverter.toDiscordMessage(components);
        DiscordConnect.getInstance().mainChannel_AddQueue(output);
    }
}
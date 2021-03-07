package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.events.GlobalChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import work.novablog.mcplugin.discordconnect.DiscordConnect;

public class ChatCasterListener implements Listener {
    /**
     * グローバルチャットに送信されたら実行（連携プラグイン有効時のみ実行される）
     * @param event チャット情報
     */
    @EventHandler
    public void onGlobalChat(GlobalChatEvent event) {
        if (event.isCancelled()) return;

        String message = DiscordConnect.getInstance().getChatCasterAPI().formatMessageForDiscord(event);
        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
        String output = MarkdownConverter.toDiscordMessage(components);
        DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel(output);
    }
}

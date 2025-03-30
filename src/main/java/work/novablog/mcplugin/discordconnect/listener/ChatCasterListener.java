package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.events.GlobalChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.event.PreForwardChatToDiscordEvent;
import work.novablog.mcplugin.discordconnect.util.BotManager;

public class ChatCasterListener implements Listener {
    private final BotManager botManager;
    private final N8ChatCasterAPI api;

    public ChatCasterListener(@NotNull BotManager botManager, @NotNull N8ChatCasterAPI api) {
        this.botManager = botManager;
        this.api = api;
    }

    @EventHandler
    public void onPreForwardChatToDiscord(PreForwardChatToDiscordEvent event) {
        // BungeeListener#onPreForwardChatToDiscordによって二重に送信されるのを抑制
        event.setCancelled(true);
    }

    /**
     * グローバルチャットに送信されたら実行（連携プラグイン有効時のみ実行される）
     *
     * @param event チャット情報
     */
    @EventHandler
    public void onGlobalChat(GlobalChatEvent event) {
        if (event.isCancelled()) return;

        String message = api.formatMessageForDiscord(event);
        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
        String output = MarkdownConverter.toDiscordMessage(components);
        botManager.sendMessageToChannel(BotManager.ChannelType.CHAT, output);
    }
}

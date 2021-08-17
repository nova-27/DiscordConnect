package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.events.GlobalChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConvertUtil;

public class ChatCasterListener implements Listener {
    /**
     * グローバルチャットに送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onGlobalChat(GlobalChatEvent event) {
        if(!DiscordConnect.getInstance().canBotBeUsed() || event.isCancelled()) return;
        assert DiscordConnect.getInstance().getChatCasterAPI() != null;
        assert DiscordConnect.getInstance().getBotManager() != null;

        String message = DiscordConnect.getInstance().getChatCasterAPI().formatMessageForDiscord(event);
        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
        String output = MarkdownConverter.toDiscordMessage(components);

        String name = event.getSender().getName();
        String avatarURL = ConvertUtil.getMinecraftAvatarURL(event.getSender().getUniqueId());

        DiscordConnect.getInstance().getDiscordWebhookSenders().forEach(sender -> sender.sendMessage(
                name,
                avatarURL,
                output
        ));
    }
}

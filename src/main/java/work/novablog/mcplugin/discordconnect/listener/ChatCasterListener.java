package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.events.GlobalChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConvertUtil;

public class ChatCasterListener implements Listener {
    private final String fromMinecraftToDiscordName;

    /**
     * ChatCasterのイベントをリッスンするインスタンスを生成します
     * @param fromMinecraftToDiscordName マイクラからDiscordへ転送するときの名前欄のフォーマット
     */
    public ChatCasterListener(@NotNull String fromMinecraftToDiscordName) {
        this.fromMinecraftToDiscordName = fromMinecraftToDiscordName;
    }

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
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);

        String name = fromMinecraftToDiscordName
                .replace("{name}", event.getSender().getName())
                .replace("{displayName}", event.getSender().getDisplayName())
                .replace("{server}", event.getSender().getServer().getInfo().getName());
        String avatarURL = ConvertUtil.getMinecraftAvatarURL(event.getSender().getUniqueId());

        DiscordConnect.getInstance().getDiscordWebhookSenders().forEach(sender -> sender.sendMessage(
                name,
                avatarURL,
                convertedMessage
        ));
    }
}

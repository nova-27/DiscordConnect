package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeeChannelChatEvent;
import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeePostJapanizeEvent;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import work.novablog.mcplugin.discordconnect.DiscordConnect;

public class LunaChatListener implements Listener {
    private String toDiscordFormat;

    public void setToDiscordFormat(String toDiscordFormat) {
        this.toDiscordFormat = toDiscordFormat;
    }

    /**
     * LunaChatのチャンネルにJapanizeメッセージが送信されたら実行
     * @param event チャット情報
     */
    @EventHandler
    public void onJapanizeChat(LunaChatBungeePostJapanizeEvent event) {
        if(!event.getChannel().isGlobalChannel()) return;

        //TODO フォーマットの自由指定
        DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel("JP: " + event.getJapanized());
    }

    /**
     * LunaChatのチャンネルにメッセージが送信されたら実行
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(LunaChatBungeeChannelChatEvent event) {
        if(!event.getChannel().isGlobalChannel()) return;

        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(event.getNgMaskedMessage(), '&');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);
        DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel(
                toDiscordFormat.replace("{server}", event.getMember().getServerName())
                        .replace("{sender}", event.getMember().getDisplayName())
                        .replace("{message}", convertedMessage)
        );
    }
}

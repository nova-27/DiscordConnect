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
    private String japanizeFormat;

    public void setToDiscordFormat(String toDiscordFormat) {
        this.toDiscordFormat = toDiscordFormat;
    }
    public void setJapanizeFormat(String japanizeFormat) {this.japanizeFormat = japanizeFormat;}

    /**
     * LunaChatのチャンネルにJapanizeメッセージが送信されたら実行
     * @param event チャット情報
     */
    @EventHandler
    public void onJapanizeChat(LunaChatBungeePostJapanizeEvent event) {
        if(!event.getChannel().isGlobalChannel()) return;

        MarkComponent[] JPcomponents = MarkdownConverter.fromMinecraftMessage(event.getJapanized(), '&');
        String JPconvertedMessage = MarkdownConverter.toDiscordMessage(JPcomponents);

        MarkComponent[] ORcomponents = MarkdownConverter.fromMinecraftMessage(event.getOriginal(), '&');
        String ORconvertedMessage = MarkdownConverter.toDiscordMessage(ORcomponents);

        DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel(
                japanizeFormat.replace("{server}", event.getMember().getServerName())
                        .replace("{sender}", event.getMember().getDisplayName())
                        .replace("{japanized}", JPconvertedMessage)
                        .replace("{original}", ORconvertedMessage)
        );
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

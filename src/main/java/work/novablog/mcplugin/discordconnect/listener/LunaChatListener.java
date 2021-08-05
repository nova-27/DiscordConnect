package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeeChannelChatEvent;
import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeePostJapanizeEvent;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;

public class LunaChatListener implements Listener {
    private final String toDiscordFormat;
    private final String japanizeFormat;

    /**
     * LunaChatのイベントをリッスンするインスタンスを生成します
     * @param toDiscordFormat プレーンメッセージをDiscordへ送信するときのフォーマット
     * @param japanizeFormat japanizeメッセージをDiscordへ送信するときのフォーマット
     */
    public LunaChatListener(@NotNull String toDiscordFormat, @NotNull String japanizeFormat) {
        this.toDiscordFormat = toDiscordFormat;
        this.japanizeFormat = japanizeFormat;
    }

    /**
     * LunaChatのチャンネルにJapanizeメッセージが送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onJapanizeChat(LunaChatBungeePostJapanizeEvent event) {
        if(!DiscordConnect.getInstance().canBotBeUsed() || !event.getChannel().isGlobalChannel()) return;
        BotManager botManager =  DiscordConnect.getInstance().getBotManager();
        assert botManager != null;

        MarkComponent[] JPcomponents = MarkdownConverter.fromMinecraftMessage(event.getJapanized(), '&');
        String JPconvertedMessage = MarkdownConverter.toDiscordMessage(JPcomponents);

        MarkComponent[] ORcomponents = MarkdownConverter.fromMinecraftMessage(event.getOriginal(), '&');
        String ORconvertedMessage = MarkdownConverter.toDiscordMessage(ORcomponents);
        botManager.sendMessageToChatChannel(
                japanizeFormat.replace("{server}", event.getMember().getServerName())
                        .replace("{sender}", event.getMember().getDisplayName())
                        .replace("{japanized}", JPconvertedMessage)
                        .replace("{original}", ORconvertedMessage)
        );
    }

    /**
     * LunaChatのチャンネルにメッセージが送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(LunaChatBungeeChannelChatEvent event) {
        if(!DiscordConnect.getInstance().canBotBeUsed() || !event.getChannel().isGlobalChannel()) return;
        BotManager botManager =  DiscordConnect.getInstance().getBotManager();
        assert DiscordConnect.getInstance().getBotManager() != null;

        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(event.getNgMaskedMessage(), '&');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);
        botManager.sendMessageToChatChannel(
                toDiscordFormat.replace("{server}", event.getMember().getServerName())
                        .replace("{sender}", event.getMember().getDisplayName())
                        .replace("{message}", convertedMessage)
        );
    }
}

package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc3.UUIDCacheData;
import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeeChannelChatEvent;
import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeePostJapanizeEvent;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConvertUtil;
import work.novablog.mcplugin.discordconnect.util.discord.WebhookManager;

import java.util.UUID;

public class LunaChatListener implements Listener {
    private final String toDiscordFormat;
    private final String japanizeFormat;
    private final UUIDCacheData uuidCacheData;

    /**
     * LunaChatのイベントをリッスンするインスタンスを生成します
     * @param toDiscordFormat プレーンメッセージをDiscordへ送信するときのフォーマット
     * @param japanizeFormat japanizeメッセージをDiscordへ送信するときのフォーマット
     * @param uuidCacheData LunaChatから取得した、UUIDのキャッシュ
     */
    public LunaChatListener(@NotNull String toDiscordFormat, @NotNull String japanizeFormat, @NotNull UUIDCacheData uuidCacheData) {
        this.toDiscordFormat = toDiscordFormat;
        this.japanizeFormat = japanizeFormat;
        this.uuidCacheData = uuidCacheData;
    }

    /**
     * LunaChatのチャンネルにJapanizeメッセージが送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onJapanizeChat(LunaChatBungeePostJapanizeEvent event) {
        WebhookManager webhookManager = DiscordConnect.getInstance().getWebhookManager();
        if(webhookManager == null || !event.getChannel().isGlobalChannel()) return;

        MarkComponent[] JPcomponents = MarkdownConverter.fromMinecraftMessage(event.getJapanized(), '&');
        String JPconvertedMessage = MarkdownConverter.toDiscordMessage(JPcomponents);

        MarkComponent[] ORcomponents = MarkdownConverter.fromMinecraftMessage(event.getOriginal(), '&');
        String ORconvertedMessage = MarkdownConverter.toDiscordMessage(ORcomponents);

        UUID playerUuid = UUID.fromString(uuidCacheData.getUUIDFromName(event.getMember().getName()));

        webhookManager.sendMessage(
                event.getMember().getDisplayName(),
                ConvertUtil.getMinecraftAvatarURL(playerUuid),
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
        WebhookManager webhookManager = DiscordConnect.getInstance().getWebhookManager();
        if(webhookManager == null || !event.getChannel().isGlobalChannel()) return;

        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(event.getNgMaskedMessage(), '&');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);

        UUID playerUuid = UUID.fromString(uuidCacheData.getUUIDFromName(event.getMember().getName()));

        webhookManager.sendMessage(
                event.getMember().getDisplayName(),
                ConvertUtil.getMinecraftAvatarURL(playerUuid),
                toDiscordFormat.replace("{server}", event.getMember().getServerName())
                        .replace("{sender}", event.getMember().getDisplayName())
                        .replace("{message}", convertedMessage)
        );
    }
}

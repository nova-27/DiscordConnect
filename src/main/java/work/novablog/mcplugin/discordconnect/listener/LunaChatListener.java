package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc3.LunaChatBungee;
import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeeChannelChatEvent;
import com.github.ucchyocean.lc3.japanize.JapanizeType;
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
     * LunaChatのチャンネルにメッセージが送信されたら実行
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(LunaChatBungeeChannelChatEvent event) {
        if(!event.getChannel().isGlobalChannel()) return;

        LunaChatBungee lunaChat = DiscordConnect.getInstance().getLunaChat();
        boolean japanese = true;

        String marker = lunaChat.getConfig().getNoneJapanizeMarker();
        if (!marker.isEmpty() && event.getPreReplaceMessage().startsWith(marker)) {
            japanese = false;
        }

        if (!lunaChat.getLunaChatAPI().isPlayerJapanize(event.getMember().getName())) {
            japanese = false;
        }

        String message;
        if(japanese) {
            String jp = lunaChat.getLunaChatAPI().japanize(event.getNgMaskedMessage(), JapanizeType.GOOGLE_IME);
            MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(jp, '&');
            message = japanizeFormat.replace("{japanized}", MarkdownConverter.toDiscordMessage(components));
        }else{
            message = toDiscordFormat;
        }
        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(event.getNgMaskedMessage(), '&');
        DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel(
                message.replace("{server}", event.getMember().getServerName())
                        .replace("{sender}", event.getMember().getDisplayName())
                        .replace("{original}", MarkdownConverter.toDiscordMessage(components))
        );
    }
}

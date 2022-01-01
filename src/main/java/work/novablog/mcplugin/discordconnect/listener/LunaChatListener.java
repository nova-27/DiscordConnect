package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc3.LunaChatBungee;
import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeeChannelChatEvent;
import com.github.ucchyocean.lc3.japanize.JapanizeType;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import work.novablog.mcplugin.discordconnect.DiscordConnect;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LunaChatListener implements Listener {
    private String toDiscordFormat;
    private String japanizeFormat;

    public void setToDiscordFormat(String toDiscordFormat) {
        this.toDiscordFormat = toDiscordFormat;
    }
    public void setJapanizeFormat(String japanizeFormat) {this.japanizeFormat = japanizeFormat;}

    /**
     * Japanize化すべきかどうか判断する
     * @param event LunaChatBungeeChannelChatEvent
     * @return 変換すべきならtrue
     */
    public boolean shouldJapanize(LunaChatBungeeChannelChatEvent event) {
        LunaChatBungee lunaChat = DiscordConnect.getInstance().getLunaChat();

        // NoneJPマーカーで始まる
        String marker = lunaChat.getConfig().getNoneJapanizeMarker();
        if (!marker.isEmpty() && event.getPreReplaceMessage().startsWith(marker)) return false;

        // jp off
        if (!lunaChat.getLunaChatAPI().isPlayerJapanize(event.getMember().getName())) return false;

        String regex = "[\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff\uff66-\uff9f]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(event.getNgMaskedMessage());
        return !m.find();
    }

    /**
     * LunaChatのチャンネルにメッセージが送信されたら実行
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(LunaChatBungeeChannelChatEvent event) {
        if(!event.getChannel().isGlobalChannel()) return;

        ProxyServer.getInstance().getScheduler().schedule(DiscordConnect.getInstance(), () -> {
            LunaChatBungee lunaChat = DiscordConnect.getInstance().getLunaChat();
            String message;
            if(shouldJapanize(event)) {
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
        }, 0L, TimeUnit.SECONDS);
    }
}

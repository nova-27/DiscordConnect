package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class DiscordListener extends ListenerAdapter {
    private final String prefix;
    private final String toMinecraftFormat;

    /**
     * Discordのイベントをリッスンするインスタンスを生成します
     * @param prefix コマンドのprefix
     * @param toMinecraftFormat DiscordのメッセージをBungeecordへ転送するときのフォーマット
     */
    public DiscordListener(String prefix, String toMinecraftFormat) {
        this.prefix = prefix;
        this.toMinecraftFormat = toMinecraftFormat;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent receivedMessage) {
        if(receivedMessage.getAuthor().isBot()) return;
        BotManager botManager = DiscordConnect.getInstance().getBotManager();
        assert botManager != null;
        if(!botManager.getChatChannelIds().contains(receivedMessage.getChannel().getIdLong())) return;

        if (receivedMessage.getMessage().getContentRaw().startsWith(prefix)) {
            //コマンド TODO
            String command = receivedMessage.getMessage().getContentRaw().replace(prefix, "").split("\\s+")[0];
            String[] args = receivedMessage.getMessage().getContentRaw().replaceAll(Pattern.quote(prefix + command) + "\\s*", "").split("\\s+");
            if(args[0].equals("")) {
                args = new String[0];
            }

            //DiscordConnect.getInstance().embed(Color.RED, "coming soon...", null);
        } else {
            //マイクラに送信
            if(!receivedMessage.getMessage().getContentRaw().equals("")) {
                MarkComponent[] components = MarkdownConverter.fromDiscordMessage(receivedMessage.getMessage().getContentRaw());
                List<BaseComponent> convertedMessage = Arrays.asList(MarkdownConverter.toMinecraftMessage(components));

                String nickname = Objects.requireNonNull(receivedMessage.getGuild().getMember(receivedMessage.getAuthor())).getNickname();
                if(nickname == null) nickname = receivedMessage.getAuthor().getName();

                TextComponent message = new TextComponent(
                        TextComponent.fromLegacyText(
                                toMinecraftFormat.replace("{channel_name}", receivedMessage.getChannel().getName())
                                        .replace("{name}", nickname)
                        )
                );
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(receivedMessage.getAuthor().getAsTag())));
                List<BaseComponent> extra = message.getExtra();
                extra.addAll(convertedMessage);
                message.setExtra(extra);

                ProxyServer.getInstance().broadcast(message);
            }

            receivedMessage.getMessage().getAttachments().forEach((attachment) -> {
                TextComponent message = new TextComponent(TextComponent.fromLegacyText(toMinecraftFormat
                        .replace("{name}", receivedMessage.getAuthor().getName())
                        .replace("{channel_name}", receivedMessage.getChannel().getName()) +
                        attachment.getUrl()
                ));
                message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()));
                ProxyServer.getInstance().broadcast(message);
            });

            //Discordに再送
            String message = receivedMessage.getMessage().getContentRaw();
            StringJoiner sj = new StringJoiner("\n");
            receivedMessage.getMessage().getAttachments().forEach(attachment -> sj.add(attachment.getUrl()));
            String finalMessage = message + "\n" + sj;
            if(!finalMessage.equals("\n")) {
                //空白でなければ送信
                DiscordConnect.getInstance().getDiscordWebhookSenders().forEach(sender ->
                        sender.sendMessage(
                                receivedMessage.getAuthor().getName(),
                                receivedMessage.getAuthor().getAvatarUrl(),
                                finalMessage
                        )
                );
            }

            //メッセージを削除
            receivedMessage.getMessage().delete().queue();
        }
    }
}

package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;

public class DiscordListener extends ListenerAdapter {
    private final String toMinecraftFormat;

    public DiscordListener(String toMinecraftFormat) {
        this.toMinecraftFormat = toMinecraftFormat;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent message) {
        if(message.getAuthor().isBot()) return;
        if(!DiscordConnect.getInstance().getBotManager().getChatChannelIds().contains(message.getChannel().getIdLong())) return;

        //メッセージ
        if(!message.getMessage().getContentRaw().equals("")) {
            MarkComponent[] components = MarkdownConverter.fromDiscordMessage(message.getMessage().getContentRaw());
            TextComponent[] convertedMessage = MarkdownConverter.toMinecraftMessage(components);

            TextComponent[] send = new TextComponent[convertedMessage.length + 1];
            send[0] = new TextComponent(toMinecraftFormat.replace("{name}", message.getAuthor().getName()).replace("{channel_name}", message.getChannel().getName()));
            System.arraycopy(convertedMessage, 0, send, 1, convertedMessage.length);

            ProxyServer.getInstance().broadcast(send);
        }

        message.getMessage().getAttachments().forEach((attachment) -> {
            TextComponent url = new TextComponent(attachment.getUrl());
            url.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()));
            ProxyServer.getInstance().broadcast(
                    new TextComponent(
                            toMinecraftFormat
                                    .replace("{name}", message.getAuthor().getName())
                                    .replace("{channel_name}", message.getChannel().getName())
                    ),
                    url
            );
        });
    }
}

package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.util.List;
import java.util.logging.Logger;

public class DiscordListener extends ListenerAdapter {
    private final Logger logger;
    private final List<Long> chatChannelIds;
    private final List<Long> dispatchCommandChannelIds;
    private final String toMinecraftFormat;

    public DiscordListener(
            @NotNull Logger logger,
            @NotNull List<Long> chatChannelIds,
            @NotNull List<Long> dispatchCommandChannelIds,
            @NotNull String toMinecraftFormat
    ) {
        this.logger = logger;
        this.chatChannelIds = chatChannelIds;
        this.dispatchCommandChannelIds = dispatchCommandChannelIds;
        this.toMinecraftFormat = toMinecraftFormat;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (dispatchCommandChannelIds.contains(event.getChannel().getIdLong())) {
            // BungeeCordでコマンド実行
            logger.info(
                    Message.dispatchCommand.toString()
                            .replace("{sender}", event.getAuthor().getName())
                            .replace("{command}", event.getMessage().getContentRaw())
            );

            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    ProxyServer.getInstance().getConsole(),
                    event.getMessage().getContentRaw()
            );

            return;
        }

        if (!chatChannelIds.contains(event.getChannel().getIdLong()))
            return;

        String[] parts = toMinecraftFormat
                .replace("{name}", event.getAuthor().getName())
                .replace("{channel_name}", event.getChannel().getName())
                .split("\\{message}", -1);

        ComponentBuilder messageBuilder = new ComponentBuilder();
        messageBuilder.append(TextComponent.fromLegacyText(parts[0]));

        // テキストメッセージ
        MarkComponent[] markComponents = MarkdownConverter.fromDiscordMessage(
                event.getMessage().getContentDisplay()
        );
        TextComponent[] convertedMessage = MarkdownConverter.toMinecraftMessage(markComponents);

        // 添付ファイル
        TextComponent[] attachments = event.getMessage().getAttachments().stream().map((attachment) -> {
            TextComponent url = new TextComponent(" §9§n[" + attachment.getFileName() + "]§r");
            url.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()));
            return url;
        }).toArray(TextComponent[]::new);

        // partsの間にテキストメッセージと添付ファイルを挿入
        for (int i = 1; i < parts.length; i++) {
            if (convertedMessage.length != 0)
                messageBuilder.append(convertedMessage, ComponentBuilder.FormatRetention.NONE);
            if (attachments.length != 0)
                messageBuilder.append(attachments, ComponentBuilder.FormatRetention.NONE);
            messageBuilder.append(TextComponent.fromLegacyText(parts[i]), ComponentBuilder.FormatRetention.NONE);
        }

        ProxyServer.getInstance().broadcast(messageBuilder.create());
    }
}

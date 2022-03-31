package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.command.DiscordCommandExecutor;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class DiscordListener extends ListenerAdapter {
    private final String prefix;
    private final String toMinecraftFormat;
    private final String fromDiscordToDiscordName;
    private final DiscordCommandExecutor discordCommandExecutor;
    private final Long consoleChannelId;
    private final Boolean allowDispatchCommandFromConsoleChannel;

    /**
     * Discordのイベントをリッスンするインスタンスを生成します
     *
     * @param prefix                   コマンドのprefix
     * @param toMinecraftFormat        DiscordのメッセージをBungeecordへ転送するときのフォーマット
     * @param fromDiscordToDiscordName Discordのメッセージを再送するときの名前欄のフォーマット
     * @param discordCommandExecutor   discordのコマンドの解析や実行を行うインスタンス
     */
    public DiscordListener(
            @NotNull String prefix,
            @NotNull String toMinecraftFormat,
            @NotNull String fromDiscordToDiscordName,
            @NotNull DiscordCommandExecutor discordCommandExecutor,
            @Nullable Long consoleChannelId,
            @Nullable Boolean allowDispatchCommandFromConsoleChannel
    ) {
        this.prefix = prefix;
        this.toMinecraftFormat = toMinecraftFormat;
        this.fromDiscordToDiscordName = fromDiscordToDiscordName;
        this.discordCommandExecutor = discordCommandExecutor;
        this.consoleChannelId = consoleChannelId;
        this.allowDispatchCommandFromConsoleChannel = allowDispatchCommandFromConsoleChannel;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent receivedMessage) {
        if (receivedMessage.getAuthor().isBot()) return;
        BotManager botManager = DiscordConnect.getInstance().getBotManager();
        assert botManager != null;
        if (botManager.getChatChannelSenders().stream()
                .noneMatch(sender -> sender.getChannelID() == receivedMessage.getChannel().getIdLong()) && !(consoleChannelId == receivedMessage.getChannel().getIdLong()))
            return;

        if (receivedMessage.getMessage().getContentRaw().startsWith(prefix)) {
            //コマンド
            String alias = receivedMessage.getMessage().getContentRaw().replace(prefix, "").split("\\s+")[0];
            String[] args = receivedMessage.getMessage().getContentRaw()
                    .replaceAll(Pattern.quote(prefix + alias) + "\\s*", "").split("\\s+");
            if (args[0].equals("")) {
                args = new String[0];
            }

            discordCommandExecutor.parse(receivedMessage, alias, args);
        } else if (consoleChannelId == receivedMessage.getChannel().getIdLong()) {
            if(Boolean.TRUE.equals(allowDispatchCommandFromConsoleChannel)) {
                String commandLine = receivedMessage.getMessage().getContentRaw();

                DiscordConnect.getInstance().getProxy().getPluginManager().dispatchCommand(
                        DiscordConnect.getInstance().getProxy().getConsole(),
                        commandLine
                );

                // 誰が何を実行したか分かりづらいのログを出力する
                DiscordConnect.getInstance().getLogger().info(
                        ConfigManager.Message.dispatchedCommand.toString()
                                .replace("{authorId}", receivedMessage.getAuthor().getId())
                                .replace("{commandLine}", commandLine)
                );
            }
        } else {
            String name = receivedMessage.getAuthor().getName();
            String nickname = Objects.requireNonNull(receivedMessage.getMember()).getNickname() == null ?
                    name : Objects.requireNonNull(receivedMessage.getMember()).getNickname();

            String formattedForMinecraft = toMinecraftFormat
                    .replace("{name}", receivedMessage.getAuthor().getName())
                    .replace("{nickName}", name)
                    .replace("{tag}", receivedMessage.getAuthor().getAsTag())
                    .replace("{server_name}", receivedMessage.getGuild().getName())
                    .replace("{channel_name}", receivedMessage.getChannel().getName());

            //マイクラに送信
            if (!receivedMessage.getMessage().getContentRaw().equals("")) {
                MarkComponent[] components =
                        MarkdownConverter.fromDiscordMessage(receivedMessage.getMessage().getContentRaw());
                List<BaseComponent> convertedMessage = Arrays.asList(MarkdownConverter.toMinecraftMessage(components));

                TextComponent message = new TextComponent(TextComponent.fromLegacyText(formattedForMinecraft));
                List<BaseComponent> extra = message.getExtra();
                extra.addAll(convertedMessage);
                message.setExtra(extra);

                ProxyServer.getInstance().broadcast(message);
            }

            receivedMessage.getMessage().getAttachments().forEach((attachment) -> {
                TextComponent url = new TextComponent(TextComponent.fromLegacyText(attachment.getUrl()));
                url.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()));
                BaseComponent[] message = new ComponentBuilder()
                        .append(formattedForMinecraft)
                        .append(url)
                        .create();
                ProxyServer.getInstance().broadcast(message);
            });

            //Discordに再送
            String nameField = fromDiscordToDiscordName
                    .replace("{name}", name)
                    .replace("{nickName}", Objects.requireNonNull(nickname))
                    .replace("{tag}", receivedMessage.getAuthor().getAsTag())
                    .replace("{server_name}", receivedMessage.getGuild().getName())
                    .replace("{channel_name}", receivedMessage.getChannel().getName());
            String message = receivedMessage.getMessage().getContentRaw();
            StringJoiner sj = new StringJoiner("\n");
            receivedMessage.getMessage().getAttachments().forEach(attachment -> sj.add(attachment.getUrl()));
            String finalMessage = message + "\n" + sj;
            if (!finalMessage.equals("\n")) {
                //空白でなければ送信
                DiscordConnect.getInstance().getDiscordWebhookSenders().forEach(sender ->
                        sender.sendMessage(
                                nameField,
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

package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class DiscordListener extends ListenerAdapter {
    private final long main_channel_id;
    private final String prefix;

    public DiscordListener(long main_channel_id, String prefix) {
        this.main_channel_id = main_channel_id;
        this.prefix = prefix;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent message) {
        //メッセージを受け取ったら
        if(message.getAuthor().isBot()) return;
        if(message.getChannel().getIdLong() != main_channel_id) return;

        if (message.getMessage().getContentRaw().startsWith(prefix)) {
            //コマンド TODO
            String command = message.getMessage().getContentRaw().replace(prefix, "").split("\\s+")[0];
            String[] args = message.getMessage().getContentRaw().replaceAll(Pattern.quote(message + command) + "\\s*", "").split("\\s+");
            if(args[0].equals("")) {
                args = new String[0];
            }

            //DiscordConnect.getInstance().embed(Color.RED, "coming soon...", null);
        }else {
            //メッセージ
            MarkComponent[] components = MarkdownConverter.fromDiscordMessage(message.getMessage().getContentRaw());
            TextComponent[] converted_message = MarkdownConverter.toMinecraftMessage(components);

            TextComponent[] send = new TextComponent[converted_message.length + 1];
            send[0] = new TextComponent(message.getAuthor().getName() + " : ");
            System.arraycopy(converted_message, 0, send, 1, converted_message.length);

            ProxyServer.getInstance().broadcast(send);
        }
    }
}

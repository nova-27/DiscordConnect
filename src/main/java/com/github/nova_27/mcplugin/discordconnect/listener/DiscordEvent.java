package com.github.nova_27.mcplugin.discordconnect.listener;

import com.github.nova_27.mcplugin.discordconnect.ConfigData;
import com.github.nova_27.mcplugin.discordconnect.DiscordConnect;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;

import java.awt.Color;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Discordイベントリスナー
 */
public class DiscordEvent implements Predicate<Event> {

    @Override
    public boolean test(Event event) {
        if(event instanceof MessageReceivedEvent){
            //メッセージを受け取ったら
            MessageReceivedEvent message = (MessageReceivedEvent) event;
            if(message.getAuthor().isBot()) return false;
            if(message.getGuild() == null) return false;
            if(message.getChannel().getIdLong() != ConfigData.mainChannelID) return false;

            String prefix = ConfigData.prefix;
            if (message.getMessage().getContentRaw().startsWith(prefix)) {
                //コマンド TODO
                String command = message.getMessage().getContentRaw().replace(prefix, "").split("\\s+")[0];
                String[] args = message.getMessage().getContentRaw().replaceAll(Pattern.quote(message + command) + "\\s*", "").split("\\s+");
                if(args[0].equals("")) {
                    args = new String[0];
                }

                DiscordConnect.getInstance().embed(Color.RED, "coming soon...", null);
            }else {
                //メッセージ
                MarkComponent[] components = MarkdownConverter.fromDiscordMessage(message.getMessage().getContentRaw());
                TextComponent[] converted_message = MarkdownConverter.toMinecraftMessage(components);

                TextComponent[] send = new TextComponent[converted_message.length + 1];
                send[0] = new TextComponent(ConfigData.toMinecraft.toString().replace("{name}", message.getAuthor().getName()));
                System.arraycopy(converted_message, 0, send, 1, converted_message.length);

                ProxyServer.getInstance().broadcast(send);
            }
        }
        return false;
    }
}

package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc3.bungee.event.LunaChatBungeeChannelMessageEvent;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.event.PreForwardChatToDiscordEvent;
import work.novablog.mcplugin.discordconnect.util.BotManager;

public class LunaChatListener implements Listener {
    private final BotManager botManager;
    private final String toDiscordFormat;

    public LunaChatListener(@NotNull BotManager botManager, @NotNull String toDiscordFormat) {
        this.botManager = botManager;
        this.toDiscordFormat = toDiscordFormat;
    }

    @EventHandler
    public void onPreForwardChatToDiscord(PreForwardChatToDiscordEvent event) {
        // BungeeListener#onPreForwardChatToDiscordによって二重に送信されるのを抑制
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLunaChatMessage(LunaChatBungeeChannelMessageEvent event) {
        if (!event.getChannel().isGlobalChannel()) return;

        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(event.getMessage(), '§');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);

        botManager.sendMessageToChannel(
                BotManager.ChannelType.CHAT,
                toDiscordFormat.replace("{server}", event.getMember().getServerName())
                        .replace("{sender}", event.getMember().getDisplayName())
                        .replace("{message}", convertedMessage)
        );
    }
}

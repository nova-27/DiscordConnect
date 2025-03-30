package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.event.PreForwardChatToDiscordEvent;
import work.novablog.mcplugin.discordconnect.util.BotManager;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BungeeListener implements Listener {
    private static final String AVATAR_IMG_URL = "https://mc-heads.net/avatar/{uuid}";
    private final BotManager botManager;
    private final String toDiscordFormat;
    private final List<String> hiddenServers;

    public BungeeListener(
            @NotNull BotManager botManager,
            @NotNull String toDiscordFormat,
            @NotNull List<String> hiddenServers
    ) {
        this.botManager = botManager;
        this.toDiscordFormat = toDiscordFormat;
        this.hiddenServers = hiddenServers;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(ChatEvent event) {
        if (event.isCommand() || event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer)) return;

        String serverName = ((ProxiedPlayer) event.getSender()).getServer().getInfo().getName();
        if (hiddenServers.contains(serverName)) return;

        PreForwardChatToDiscordEvent preEvent = new PreForwardChatToDiscordEvent(event);
        ProxyServer.getInstance().getPluginManager().callEvent(preEvent);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreForwardChatToDiscord(PreForwardChatToDiscordEvent event) {
        if (event.isCancelled()) return;

        ProxiedPlayer sender = event.getSender();
        Server server = sender.getServer();
        String message = event.getMessage();

        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);
        botManager.sendMessageToChannel(
                BotManager.ChannelType.CHAT,
                toDiscordFormat.replace("{server}", server.getInfo().getName())
                        .replace("{sender}", sender.getDisplayName())
                        .replace("{message}", convertedMessage)
        );
    }

    @EventHandler
    public void onLogin(LoginEvent e) {
        botManager.sendMessageToChannel(
                BotManager.ChannelType.CHAT,
                Message.userActivity.toString(),
                null,
                Message.joined.toString().replace("{name}", e.getConnection().getName()),
                Color.GREEN,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace(
                        "{uuid}",
                        e.getConnection().getUniqueId().toString().replace("-", "")
                )
        );

        updatePlayerCount();
    }

    @EventHandler
    public void onLogout(PlayerDisconnectEvent e) {
        botManager.sendMessageToChannel(
                BotManager.ChannelType.CHAT,
                Message.userActivity.toString(),
                null,
                Message.left.toString().replace("{name}", e.getPlayer().getName()),
                Color.RED,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace(
                        "{uuid}",
                        e.getPlayer().getUniqueId().toString().replace("-", "")
                )
        );

        updatePlayerCount();
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent e) {
        if (hiddenServers.contains(e.getPlayer().getServer().getInfo().getName())) return;

        botManager.sendMessageToChannel(
                BotManager.ChannelType.CHAT,
                Message.userActivity.toString(),
                null,
                Message.serverSwitched.toString()
                        .replace("{name}", e.getPlayer().getName())
                        .replace("{server}", e.getPlayer().getServer().getInfo().getName()),
                Color.CYAN,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace(
                        "{uuid}",
                        e.getPlayer().getUniqueId().toString().replace("-", "")
                )
        );
    }

    /**
     * プレイヤー数情報を更新
     */
    private void updatePlayerCount() {
        botManager.updateGameName(
                ProxyServer.getInstance().getOnlineCount(),
                ProxyServer.getInstance().getConfig().getPlayerLimit()
        );
    }
}

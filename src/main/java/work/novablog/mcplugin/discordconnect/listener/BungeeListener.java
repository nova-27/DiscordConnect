package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BungeeListener implements Listener {
    private static final String AVATAR_IMG_URL = "https://crafatar.com/avatars/{uuid}?size=512&default=MHF_Steve&overlay";
    private final String toDiscordFormat;

    /**
     * bungeecordのイベントを受け取るインスタンスを生成します
     * @param toDiscordFormat プレーンメッセージをDiscordへ送信するときのフォーマット
     */
    public BungeeListener(String toDiscordFormat) {
        this.toDiscordFormat = toDiscordFormat;
    }

    /**
     * チャットが送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(ChatEvent event) {
        if(!DiscordConnect.getInstance().canBotBeUsed() || event.isCommand() || event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer)) return;
        BotManager botManager = DiscordConnect.getInstance().getBotManager();
        assert botManager != null;

        N8ChatCasterAPI chatCasterApi = DiscordConnect.getInstance().getChatCasterAPI();
        LunaChatAPI lunaChatAPI = DiscordConnect.getInstance().getLunaChatAPI();
        if ((chatCasterApi == null || !chatCasterApi.isEnabledChatCaster()) && lunaChatAPI == null) {
            // 連携プラグインが無効の場合
            ProxiedPlayer sender = (ProxiedPlayer)event.getSender();
            String senderServer = sender.getServer().getInfo().getName();
            String message = event.getMessage();

            MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
            String convertedMessage = MarkdownConverter.toDiscordMessage(components);
            botManager.sendMessageToChatChannel(
                    toDiscordFormat.replace("{server}", senderServer)
                            .replace("{sender}", sender.getName())
                            .replace("{message}", convertedMessage)
            );
        }
    }

    /**
     * プレイヤーがログインしたら実行されます
     * @param e ログイン情報
     */
    @EventHandler
    public void onLogin(LoginEvent e) {
        if(!DiscordConnect.getInstance().canBotBeUsed()) return;
        BotManager botManager = DiscordConnect.getInstance().getBotManager();
        assert botManager != null;

        botManager.sendMessageToChatChannel(
                ConfigManager.Message.userActivity.toString(),
                null,
                ConfigManager.Message.joined.toString().replace("{name}", e.getConnection().getName()),
                Color.GREEN,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace("{uuid}", e.getConnection().getUniqueId().toString().replace("-", ""))
        );

        updatePlayerCount(botManager);
    }

    /**
     * プレイヤーが切断したら実行されます
     * @param e 切断情報
     */
    @EventHandler
    public void onLogout(PlayerDisconnectEvent e) {
        if(!DiscordConnect.getInstance().canBotBeUsed()) return;
        BotManager botManager = DiscordConnect.getInstance().getBotManager();
        assert botManager != null;

        botManager.sendMessageToChatChannel(
                ConfigManager.Message.userActivity.toString(),
                null,
                ConfigManager.Message.left.toString().replace("{name}", e.getPlayer().getName()),
                Color.RED,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace("{uuid}", e.getPlayer().getUniqueId().toString().replace("-", ""))
        );

        updatePlayerCount(botManager);
    }

    /**
     * プレイヤーがサーバー間を移動したら実行されます
     * @param e プレイヤー情報
     */
    @EventHandler
    public void onSwitch(ServerSwitchEvent e) {
        if(!DiscordConnect.getInstance().canBotBeUsed()) return;
        BotManager botManager = DiscordConnect.getInstance().getBotManager();
        assert botManager != null;

        botManager.sendMessageToChatChannel(
                ConfigManager.Message.userActivity.toString(),
                null,
                ConfigManager.Message.serverSwitched.toString().replace("{name}", e.getPlayer().getName()).replace("{server}", e.getPlayer().getServer().getInfo().getName()),
                Color.CYAN,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace("{uuid}", e.getPlayer().getUniqueId().toString().replace("-", ""))
        );
    }

    /**
     * プレイヤー数情報を更新します
     * @param botManager アクティブなbotマネージャーのインスタンス
     */
    private void updatePlayerCount(@NotNull BotManager botManager) {
        DiscordConnect.getInstance().getProxy().getScheduler().schedule(DiscordConnect.getInstance(), () ->
                botManager.updateGameName(
                        DiscordConnect.getInstance().getProxy().getPlayers().size(),
                        DiscordConnect.getInstance().getProxy().getConfig().getPlayerLimit()
        ),1L, TimeUnit.SECONDS);
    }
}

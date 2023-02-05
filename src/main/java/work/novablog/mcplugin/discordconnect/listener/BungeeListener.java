package work.novablog.mcplugin.discordconnect.listener;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.github.ucchyocean.lc3.LunaChatAPI;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.ConvertUtil;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordWebhookSender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class BungeeListener implements Listener {
    private final String fromMinecraftToDiscordName;
    private final List<String> hiddenServers;
    private final String dummyServerName;

    /**
     * bungeecordのイベントを受け取るインスタンスを生成します
     * @param fromMinecraftToDiscordName マイクラからDiscordへ転送するときの名前欄のフォーマット
     * @param hiddenServers
     * @param dummyServerName
     */
    public BungeeListener(@NotNull String fromMinecraftToDiscordName, List<String> hiddenServers, String dummyServerName) {
        this.fromMinecraftToDiscordName = fromMinecraftToDiscordName;
        this.hiddenServers = hiddenServers;
        this.dummyServerName = dummyServerName;
    }

    /**
     * チャットが送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(ChatEvent event) {
        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();
        if(event.isCommand() || event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer)) return;

        N8ChatCasterAPI chatCasterApi = DiscordConnect.getInstance().getChatCasterAPI();
        LunaChatAPI lunaChatAPI = DiscordConnect.getInstance().getLunaChatAPI();
        if ((chatCasterApi == null || !chatCasterApi.isEnabledChatCaster()) && lunaChatAPI == null) {
            // 連携プラグインが無効の場合
            String name = fromMinecraftToDiscordName
                    .replace("{name}", ((ProxiedPlayer) event.getSender()).getName())
                    .replace("{displayName}", ((ProxiedPlayer)event.getSender()).getDisplayName())
                    .replace("{server}", replaceServerDisplayName(((ProxiedPlayer)event.getSender()).getServer()).orElse("unknown"));

            String avatarURL = ConvertUtil.getMinecraftAvatarURL(((ProxiedPlayer) event.getSender()).getUniqueId());

            MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(event.getMessage(), '&');
            String convertedMessage = MarkdownConverter.toDiscordMessage(components);

            discordWebhookSenders.forEach(sender -> sender.sendMessage(
                    name,
                    avatarURL,
                    convertedMessage
            ));
        }
    }

    /**
     * プレイヤーがログインしたら実行されます
     * @param e ログイン情報
     */
    @EventHandler
    public void onLogin(LoginEvent e) {
        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();

        String name = fromMinecraftToDiscordName
                .replace("{name}", e.getConnection().getName())
                .replace("{displayName}", e.getConnection().getName())
                .replace("{server}", "unknown");
        String avatarURL = ConvertUtil.getMinecraftAvatarURL(e.getConnection().getUniqueId());

        WebhookEmbedBuilder webhookEmbedBuilder = new WebhookEmbedBuilder();
        webhookEmbedBuilder.setAuthor(
                new WebhookEmbed.EmbedAuthor(e.getConnection().getName(), avatarURL, null)
        );
        webhookEmbedBuilder.setColor(Color.GREEN.getRGB());
        webhookEmbedBuilder.setTitle(
                new WebhookEmbed.EmbedTitle(
                        ConfigManager.Message.userActivity.toString(),
                        null
                )
        );
        webhookEmbedBuilder.setDescription(
                ConfigManager.Message.joined.toString().replace("{name}", e.getConnection().getName())
        );

        discordWebhookSenders.forEach(sender -> sender.sendMessage(
                name,
                avatarURL,
                webhookEmbedBuilder.build()
        ));

        if(DiscordConnect.getInstance().canBotBeUsed()) {
            assert DiscordConnect.getInstance().getBotManager() != null;
            updatePlayerCount(DiscordConnect.getInstance().getBotManager());
        }
    }

    /**
     * プレイヤーが切断したら実行されます
     * @param e 切断情報
     */
    @EventHandler
    public void onLogout(PlayerDisconnectEvent e) {
        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();

        String name = fromMinecraftToDiscordName
                .replace("{name}", e.getPlayer().getName())
                .replace("{displayName}", e.getPlayer().getDisplayName())
                .replace("{server}", replaceServerDisplayName(e.getPlayer().getServer()).orElse("unknown"));

        String avatarURL = ConvertUtil.getMinecraftAvatarURL(e.getPlayer().getUniqueId());

        WebhookEmbedBuilder webhookEmbedBuilder = new WebhookEmbedBuilder();
        webhookEmbedBuilder.setAuthor(
                new WebhookEmbed.EmbedAuthor(e.getPlayer().getName(), avatarURL, null)
        );
        webhookEmbedBuilder.setColor(Color.RED.getRGB());
        webhookEmbedBuilder.setTitle(
                new WebhookEmbed.EmbedTitle(
                        ConfigManager.Message.userActivity.toString(),
                        null
                )
        );
        webhookEmbedBuilder.setDescription(
                ConfigManager.Message.left.toString().replace("{name}", e.getPlayer().getName())
        );

        discordWebhookSenders.forEach(sender -> sender.sendMessage(
                name,
                avatarURL,
                webhookEmbedBuilder.build()
        ));

        if(DiscordConnect.getInstance().canBotBeUsed()) {
            assert DiscordConnect.getInstance().getBotManager() != null;
            updatePlayerCount(DiscordConnect.getInstance().getBotManager());
        }
    }

    /**
     * プレイヤーがサーバー間を移動したら実行されます
     * @param e プレイヤー情報
     */
    @EventHandler
    public void onSwitch(ServerSwitchEvent e) {
        if(hiddenServers.contains(e.getPlayer().getServer().getInfo().getName())) return;

        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();

        String name = fromMinecraftToDiscordName
                .replace("{name}", e.getPlayer().getName())
                .replace("{displayName}", e.getPlayer().getDisplayName())
                .replace("{server}", replaceServerDisplayName(e.getPlayer().getServer()).orElse("unknown"));

        String avatarURL = ConvertUtil.getMinecraftAvatarURL(e.getPlayer().getUniqueId());

        WebhookEmbedBuilder webhookEmbedBuilder = new WebhookEmbedBuilder();
        webhookEmbedBuilder.setAuthor(
                new WebhookEmbed.EmbedAuthor(e.getPlayer().getName(), avatarURL, null)
        );
        webhookEmbedBuilder.setColor(Color.CYAN.getRGB());
        webhookEmbedBuilder.setTitle(
                new WebhookEmbed.EmbedTitle(
                        ConfigManager.Message.userActivity.toString(),
                        null
                )
        );
        webhookEmbedBuilder.setDescription(
                ConfigManager.Message.serverSwitched.toString()
                        .replace("{name}", e.getPlayer().getName())
                        .replace("{server}", replaceServerDisplayName(e.getPlayer().getServer()).orElse("unknown"))
        );

        discordWebhookSenders.forEach(sender -> sender.sendMessage(
                name,
                avatarURL,
                webhookEmbedBuilder.build()
        ));
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

    /**
     * サーバーの表示名を返します
     * @param server 対象のサーバー
     * @return サーバーの表示名。無ければ {@link Optional#empty()} を返します。
     */
    private Optional<String> replaceServerDisplayName(@Nullable Server server) {
        return Optional.ofNullable(server)
                .map(Server::getInfo)
                .map(ServerInfo::getName)
                .map(name -> hiddenServers.contains(name) ? dummyServerName : name);
    }
}

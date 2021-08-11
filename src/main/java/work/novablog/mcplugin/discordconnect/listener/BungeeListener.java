package work.novablog.mcplugin.discordconnect.listener;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.github.ucchyocean.lc3.LunaChatAPI;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.ConvertUtil;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordWebhookSender;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BungeeListener implements Listener {
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
        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();
        if(event.isCommand() || event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer)) return;

        N8ChatCasterAPI chatCasterApi = DiscordConnect.getInstance().getChatCasterAPI();
        LunaChatAPI lunaChatAPI = DiscordConnect.getInstance().getLunaChatAPI();
        if ((chatCasterApi == null || !chatCasterApi.isEnabledChatCaster()) && lunaChatAPI == null) {
            // 連携プラグインが無効の場合
            String name = ((ProxiedPlayer)event.getSender()).getName();
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

        String name = e.getConnection().getName();
        String avatarURL = ConvertUtil.getMinecraftAvatarURL(e.getConnection().getUniqueId());

        WebhookEmbedBuilder webhookEmbedBuilder = new WebhookEmbedBuilder();
        webhookEmbedBuilder.setAuthor(
                new WebhookEmbed.EmbedAuthor(name, avatarURL, null)
        );
        webhookEmbedBuilder.setColor(Color.GREEN.getRGB());
        webhookEmbedBuilder.setTitle(
                new WebhookEmbed.EmbedTitle(
                        ConfigManager.Message.userActivity.toString(),
                        null
                )
        );
        webhookEmbedBuilder.setDescription(
                ConfigManager.Message.joined.toString().replace("{name}", name)
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

        String name = e.getPlayer().getName();
        String avatarURL = ConvertUtil.getMinecraftAvatarURL(e.getPlayer().getUniqueId());

        WebhookEmbedBuilder webhookEmbedBuilder = new WebhookEmbedBuilder();
        webhookEmbedBuilder.setAuthor(
                new WebhookEmbed.EmbedAuthor(name, avatarURL, null)
        );
        webhookEmbedBuilder.setColor(Color.RED.getRGB());
        webhookEmbedBuilder.setTitle(
                new WebhookEmbed.EmbedTitle(
                        ConfigManager.Message.userActivity.toString(),
                        null
                )
        );
        webhookEmbedBuilder.setDescription(
                ConfigManager.Message.left.toString().replace("{name}", name)
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
        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();

        ProxiedPlayer player = e.getPlayer();
        String avatarURL = ConvertUtil.getMinecraftAvatarURL(e.getPlayer().getUniqueId());

        WebhookEmbedBuilder webhookEmbedBuilder = new WebhookEmbedBuilder();
        webhookEmbedBuilder.setAuthor(
                new WebhookEmbed.EmbedAuthor(player.getName(), avatarURL, null)
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
                        .replace("{name}", player.getName())
                        .replace("{server}", player.getServer().getInfo().getName())
        );

        discordWebhookSenders.forEach(sender -> sender.sendMessage(
                player.getName(),
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
}

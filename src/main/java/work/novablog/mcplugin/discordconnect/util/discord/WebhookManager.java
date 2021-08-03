package work.novablog.mcplugin.discordconnect.util.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.Message;

public class WebhookManager {
    private WebhookClient client;
    private boolean isActive;

    /**
     * Webhookへメッセージを送信するためのクラスを生成する
     * @param url WebhookのURL
     */
    public WebhookManager(@NotNull String url) {
        WebhookClientBuilder builder;
        try {
            builder = new WebhookClientBuilder(url);
        } catch (Exception e) {
            DiscordConnect.getInstance().getLogger().severe(Message.invalidWebhookURL.toString());
            client = null;
            isActive = false;
            return;
        }

        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("Discord Webhook");
            thread.setDaemon(true);
            return thread;
        });
        builder.setWait(false);
        client = builder.build();
        isActive = true;
    }

    /**
     * Webhookでプレーンメッセージを送信する
     * @param userName 送信者の名前
     * @param avatarUrl 送信者のアバターURL
     * @param message 送信するメッセージ
     * @return 送信できたかどうか
     */
    public boolean sendMessage(@Nullable String userName, @Nullable String avatarUrl, @NotNull String message) {
        if(!isActive) return false;

        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(userName);
        builder.setAvatarUrl(avatarUrl);
        builder.setContent(message);
        client.send(builder.build());

        return true;
    }

    /**
     * Webhookで埋め込みメッセージを送信する
     * @param embedMessage 送信する埋め込みメッセージ
     * @return 送信できたかどうか
     */
    public boolean sendMessage(@NotNull WebhookEmbed embedMessage) {
        if(!isActive) return false;

        client.send(embedMessage);
        return true;
    }

    /**
     * Webhookのスレッドを停止する
     */
    public void shutdown() {
        if(!isActive) return;
        client.close();
    }
}

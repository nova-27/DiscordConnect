package work.novablog.mcplugin.discordconnect.util.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebhookManager {
    private WebhookClient client;

    /**
     * DiscordのWebhookにメッセージを送信するためのクラスを生成します
     * @param url WebhookのURL
     * @throws IllegalArgumentException URLの形式が不正な場合
     */
    public WebhookManager(@NotNull String url) throws IllegalArgumentException {
        WebhookClientBuilder builder = new WebhookClientBuilder(url);

        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("Discord Webhook");
            thread.setDaemon(true);
            return thread;
        });
        builder.setWait(false);
        client = builder.build();
    }

    /**
     * Webhookでプレーンメッセージを送信します
     * @param userName 送信者の名前
     * @param avatarUrl 送信者のアバターURL
     * @param message 送信するプレーンメッセージ
     */
    public void sendMessage(@Nullable String userName, @Nullable String avatarUrl, @NotNull String message) {
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(userName);
        builder.setAvatarUrl(avatarUrl);
        builder.setContent(message);
        client.send(builder.build());
    }

    /**
     * Webhookで埋め込みメッセージを送信します
     * @param embedMessage 送信する埋め込みメッセージ
     */
    public void sendMessage(@NotNull WebhookEmbed embedMessage) {
        client.send(embedMessage);
    }

    /**
     * Webhookのスレッドを停止します
     */
    public void shutdown() {
        client.close();
    }
}

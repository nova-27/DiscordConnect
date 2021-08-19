package work.novablog.mcplugin.discordconnect.util.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.listener.DiscordListener;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

/**
 * DiscordBotの管理を行う
 */
public class BotManager implements EventListener {
    private final JDA bot;
    private final List<Long> chatChannelIds;
    private List<DiscordBotSender> chatChannelSenders;
    private final String playingGameName;

    private boolean isActive;
    private static boolean isRestarting = false;

    /**
     * discordのbotでメッセージを送信するためのbot管理インスタンスを生成します
     * @param token botのトークン
     * @param chatChannelIds BungeeCordのチャットと連携されるDiscordチャンネルのID
     * @param playingGameName botのステータスに表示されるプレイ中のゲーム名
     * @param prefix コマンドのprefix
     * @param toMinecraftFormat DiscordのメッセージをBungeeCordに転送するときのフォーマット
     * @throws LoginException botのログインに失敗した場合にthrowされます
     */
    public BotManager(@NotNull String token, @NotNull List<Long> chatChannelIds, @NotNull String playingGameName, @NotNull String prefix, @NotNull String toMinecraftFormat) throws LoginException {
        //ログインする
        bot = JDABuilder.create(token, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(this)
                .setAutoReconnect(true)
                .build();
        bot.addEventListener(new DiscordListener(prefix, toMinecraftFormat));

        this.chatChannelIds = chatChannelIds;
        this.playingGameName = playingGameName;
        this.isActive = false;
    }

    /**
     * botをシャットダウンします
     * @param isRestart botの再起動(reload)かどうか
     *                  trueの場合、プロキシの開始・停止メッセージが表示されません
     */
    public void botShutdown(boolean isRestart) {
        if(!isActive) return;

        isActive = false;
        isRestarting = isRestart;
        DiscordConnect.getInstance().getLogger().info(ConfigManager.Message.normalShutdown.toString());

        if(!isRestart && chatChannelSenders != null) {
            //プロキシ終了メッセージ
            sendMessageToChatChannel(
                    ConfigManager.Message.serverActivity.toString(),
                    null,
                    ConfigManager.Message.proxyStopped.toString(),
                    new Color(102, 205, 170),
                    new ArrayList<>(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                );
            //送信完了を待つ
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //送信完了まで待機
        if(chatChannelSenders != null) {
            chatChannelSenders.forEach(DiscordBotSender::interrupt);
            chatChannelSenders.forEach(sender -> {
                try {
                    sender.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        bot.shutdownNow();
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if(event instanceof ReadyEvent) {
            //Botのログインが完了

            //チャットチャンネルを探す
            chatChannelSenders = chatChannelIds.stream()
                    .map(id -> {
                        TextChannel chatChannel = bot.getTextChannelById(id);
                        return chatChannel == null ? null : new DiscordBotSender(chatChannel);
                    })
                    .collect(Collectors.toList());
            if(chatChannelSenders.contains(null)) {
                //無効なチャンネルがあればシャットダウン
                DiscordConnect.getInstance().getLogger().severe(ConfigManager.Message.mainChannelNotFound.toString());
                DiscordConnect.getInstance().getLogger().severe(ConfigManager.Message.shutdownDueToError.toString());
                bot.shutdownNow();
                return;
            }

            chatChannelSenders.forEach(Thread::start);
            updateGameName(
                    DiscordConnect.getInstance().getProxy().getPlayers().size(),
                    DiscordConnect.getInstance().getProxy().getConfig().getPlayerLimit()
            );
            DiscordConnect.getInstance().getLogger().info(ConfigManager.Message.botIsReady.toString());

            isActive = true;

            if(isRestarting) {
                isRestarting = false;
                DiscordConnect.getInstance().getLogger().info(ConfigManager.Message.botRestarted.toString());
                return;
            }

            sendMessageToChatChannel(
                    ConfigManager.Message.serverActivity.toString(),
                    null,
                    ConfigManager.Message.proxyStarted.toString(),
                    new Color(102, 205, 170),
                    new ArrayList<>(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );}
    }

    /**
     * botがアクティブかどうか返します
     * <p>
     *     非アクティブの時botはログインしていないため、メッセージ送信など各種機能を利用できません
     * </p>
     * @return trueの場合アクティブ
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * チャットチャンネルへメッセージを送信します
     * @param mes 送信するメッセージ
     */
    public void sendMessageToChatChannel(@NotNull String mes) {
        chatChannelSenders.forEach(sender -> sender.addQueue(mes));
    }

    /**
     * チャットチャンネルへ埋め込みメッセージを送信
     * @param title タイトル
     * @param titleUrl タイトルのリンクURL
     * @param desc 説明
     * @param color 色
     * @param embedFields フィールド
     * @param author 送信者の名前
     * @param authorUrl 送信者のリンクURL
     * @param authorIcon 送信者のアイコン
     * @param footer フッター
     * @param footerIcon フッターのアイコン
     * @param image 画像
     * @param thumbnail サムネイル
     */
    public void sendMessageToChatChannel(String title, String titleUrl, String desc, Color color, @NotNull List<MessageEmbed.Field> embedFields, String author, String authorUrl, String authorIcon, String footer, String footerIcon, String image, String thumbnail) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(title, titleUrl);
        eb.setColor(color);
        eb.setDescription(desc);
        embedFields.forEach(eb::addField);
        eb.setAuthor(author, authorUrl, authorIcon);
        eb.setFooter(footer, footerIcon);
        eb.setImage(image);
        eb.setThumbnail(thumbnail);

        chatChannelSenders.forEach(sender -> sender.addQueue(eb.build()));
    }

    /**
     * チャットチャンネルのIDリストを取得します
     * @return IDリスト
     */
    public List<Long> getChatChannelIds() {
        return chatChannelIds;
    }

    /**
     * botのプレイ中のゲーム名を更新します
     * @param playerCount プレイヤー数
     * @param maxPlayers 最大プレイヤー数
     */
    public void updateGameName(int playerCount, int maxPlayers) {
        String maxPlayersString = maxPlayers != -1 ? String.valueOf(maxPlayers) : "∞";

        bot.getPresence().setActivity(
                Activity.playing(playingGameName
                        .replace("{players}", String.valueOf(playerCount))
                        .replace("{max}", maxPlayersString)
                )
        );
    }
}
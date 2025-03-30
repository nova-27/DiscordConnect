package work.novablog.mcplugin.discordconnect.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.ProxyServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.listener.DiscordListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DiscordBotの管理を行う
 */
public class BotManager implements EventListener {
    private final Logger logger;
    private JDA bot;
    private final HashMap<ChannelType, List<Long>> channelIds;
    private final HashMap<ChannelType, List<DiscordSender>> channelSenders;
    private final String playingGameName;

    private boolean isActive;

    public BotManager(
            @NotNull Logger logger,
            @NotNull String token,
            @NotNull List<Long> chatChannelIds,
            @NotNull List<Long> consoleChannelIds,
            boolean allowConsoleChannelDispatchCommand,
            @NotNull String playingGameName,
            @NotNull String toMinecraftFormat
    ) {
        this.logger = logger;

        //ログインする
        try {
            bot = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this, new DiscordListener(
                            logger,
                            chatChannelIds,
                            allowConsoleChannelDispatchCommand ? consoleChannelIds : Collections.emptyList(),
                            toMinecraftFormat
                    )).build();
            isActive = true;
        } catch (InvalidTokenException e) {
            this.logger.severe(Message.invalidToken.toString());
            bot = null;
            isActive = false;
        }

        this.channelIds = new HashMap<>();
        this.channelIds.put(ChannelType.CHAT, chatChannelIds);
        this.channelSenders = new HashMap<>();
        this.channelIds.put(ChannelType.CONSOLE, consoleChannelIds);
        this.channelSenders.put(ChannelType.CHAT, new ArrayList<>());
        this.channelSenders.put(ChannelType.CONSOLE, new ArrayList<>());
        this.playingGameName = playingGameName;
    }

    /**
     * botをシャットダウンする
     * これを呼んだあとはこのインスタンスを利用してはならない
     */
    public void botShutdown() {
        if (!isActive) return;

        logger.info(Message.normalShutdown.toString());

        //プロキシ停止メッセージ
        sendMessageToChannel(
                ChannelType.ALL,
                Message.serverActivity.toString(),
                null,
                Message.proxyStopped.toString(),
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

        //送信完了まで待機
        for (List<DiscordSender> senders : channelSenders.values()) {
            senders.forEach(DiscordSender::interrupt);
            senders.forEach(sender -> {
                try {
                    sender.join();
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Exception", e);
                }
            });
        }

        //botのシャットダウン
        bot.shutdown();

        bot = null;
        channelSenders.clear();
        isActive = false;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ReadyEvent) {
            //Botのログインが完了

            //channelSendersの設定
            for (ChannelType type : channelIds.keySet()) {
                List<Long> ids = this.channelIds.get(type);
                List<DiscordSender> senders = channelSenders.get(type);

                for (long id : ids) {
                    TextChannel channel = bot.getTextChannelById(id);

                    if (channel == null) {
                        logger.warning(Message.channelNotFound.toString().replace("{id}", String.valueOf(id)));
                        continue;
                    }

                    DiscordSender sender = new DiscordSender(channel);
                    sender.start();
                    senders.add(sender);
                }
            }

            updateGameName(
                    ProxyServer.getInstance().getPlayers().size(),
                    ProxyServer.getInstance().getConfig().getPlayerLimit()
            );

            sendMessageToChannel(
                    ChannelType.ALL,
                    Message.serverActivity.toString(),
                    null,
                    Message.proxyStarted.toString(),
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

            logger.info(Message.botIsReady.toString());
        }
    }

    /**
     * テキストチャンネルへメッセージを送信
     *
     * @param channelType チャンネルの種類
     * @param mes         メッセージ
     */
    public void sendMessageToChannel(@NotNull ChannelType channelType, @NotNull String mes) {
        if (channelType == ChannelType.ALL) {
            channelSenders.values().forEach(senders -> senders.forEach(sender -> sender.addQueue(mes)));
        } else {
            channelSenders.getOrDefault(channelType, new ArrayList<>()).forEach(sender -> sender.addQueue(mes));
        }
    }

    /**
     * テキストチャンネルへ埋め込みメッセージを送信
     *
     * @param channelType チャンネルの種類
     * @param title       タイトル
     * @param titleUrl    タイトルのリンクURL
     * @param desc        説明
     * @param color       色
     * @param embedFields フィールド
     * @param author      送信者の名前
     * @param authorUrl   送信者のリンクURL
     * @param authorIcon  送信者のアイコン
     * @param footer      フッター
     * @param footerIcon  フッターのアイコン
     * @param image       画像
     * @param thumbnail   サムネイル
     */
    public void sendMessageToChannel(
            @NotNull ChannelType channelType,
            @Nullable String title,
            @Nullable String titleUrl,
            @Nullable String desc,
            @Nullable Color color,
            @NotNull List<MessageEmbed.Field> embedFields,
            @Nullable String author,
            @Nullable String authorUrl,
            @Nullable String authorIcon,
            @Nullable String footer,
            @Nullable String footerIcon,
            @Nullable String image,
            @Nullable String thumbnail
    ) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(title, titleUrl);
        eb.setDescription(desc);
        eb.setColor(color);
        embedFields.forEach(eb::addField);
        eb.setAuthor(author, authorUrl, authorIcon);
        eb.setFooter(footer, footerIcon);
        eb.setImage(image);
        eb.setThumbnail(thumbnail);

        if (channelType == ChannelType.ALL) {
            channelSenders.values().forEach(senders -> senders.forEach(sender -> sender.addQueue(eb.build())));
        } else {
            channelSenders.getOrDefault(channelType, new ArrayList<>()).forEach(sender -> sender.addQueue(eb.build()));
        }
    }

    /**
     * プレイ中のゲーム名を更新
     *
     * @param playerCount プレイヤー数
     * @param maxPlayers  最大プレイヤー数
     */
    public void updateGameName(int playerCount, int maxPlayers) {
        if (!isActive) return;

        String maxPlayersString = maxPlayers != -1 ? String.valueOf(maxPlayers) : "∞";

        bot.getPresence().setActivity(
                Activity.playing(playingGameName
                        .replace("{players}", String.valueOf(playerCount))
                        .replace("{max}", maxPlayersString)));
    }

    public enum ChannelType {
        CHAT, CONSOLE, ALL
    }
}
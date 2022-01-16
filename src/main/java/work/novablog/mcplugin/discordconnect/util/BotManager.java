package work.novablog.mcplugin.discordconnect.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.command.DiscordCommandExecutor;
import work.novablog.mcplugin.discordconnect.command.DiscordStandardCommands;
import work.novablog.mcplugin.discordconnect.listener.ChatCasterListener;
import work.novablog.mcplugin.discordconnect.listener.DiscordListener;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DiscordBotの管理を行う
 */
public class BotManager implements EventListener {
    private JDA bot;
    private List<Long> chatChannelIds;
    private List<DiscordSender> chatChannelSenders;
    private String playingGameName;

    private boolean isActive;
    private static boolean isRestarting;

    public BotManager(
            String token,
            List<Long> chatChannelIds,
            String playingGameName,
            String globalCmdAlias,
            String toMinecraftFormat,
            String adminRole
    ) {
        //ログインする
        try {
            bot = JDABuilder.createDefault(token)
                    .addEventListeners(this)
                    .setAutoReconnect(true)
                    .build();
            bot.addEventListener(new DiscordListener(toMinecraftFormat));
            DiscordCommandExecutor discordCommandExecutor = new DiscordCommandExecutor(globalCmdAlias, adminRole, bot);
            discordCommandExecutor.registerCommand(new DiscordStandardCommands());
            bot.addEventListener(discordCommandExecutor);
        } catch (LoginException e) {
            DiscordConnect.getInstance().getLogger().severe(Message.invalidToken.toString());
            bot = null;
            isActive = false;
            return;
        }

        this.chatChannelIds = chatChannelIds;
        this.playingGameName = playingGameName;
        isActive = true;
    }

    /**
     * botをシャットダウンする
     * @param isRestart botの再起動(reload)か
     */
    public void botShutdown(boolean isRestart) {
        if(!isActive) return;

        DiscordConnect.getInstance().getProxy().getPluginManager().unregisterListener(DiscordConnect.getInstance().getBungeeListener());
        ChatCasterListener chatCasterListener = DiscordConnect.getInstance().getChatCasterListener();
        LunaChatListener lunaChatListener = DiscordConnect.getInstance().getLunaChatListener();
        if(chatCasterListener != null)  DiscordConnect.getInstance().getProxy().getPluginManager().unregisterListener(chatCasterListener);
        if(lunaChatListener != null)  DiscordConnect.getInstance().getProxy().getPluginManager().unregisterListener(lunaChatListener);
        DiscordConnect.getInstance().getLogger().info(Message.normalShutdown.toString());

        if(isRestart) {
            bot.shutdownNow();
            isRestarting = true;
        }else{
            //プロキシ停止メッセージ
            if(chatChannelSenders != null) {
                sendMessageToChatChannel(
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
            }

            //送信完了まで待機
            if(chatChannelSenders != null) {
                chatChannelSenders.forEach(DiscordSender::threadStop);
                chatChannelSenders.forEach(sender -> {
                    try {
                        sender.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                chatChannelSenders = null;
            }

            //botのシャットダウン
            bot.shutdown();
        }

        bot = null;
        chatChannelSenders = null;
        chatChannelIds = null;
        isActive = false;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if(event instanceof ReadyEvent) {
            //Botのログインが完了

            //チャットチャンネルを探す
            chatChannelSenders = chatChannelIds.stream()
                    .map(id -> {
                        TextChannel chatChannel = bot.getTextChannelById(id);
                        if(chatChannel == null) {
                            return null;
                        }else{
                            return new DiscordSender(chatChannel);
                        }
                    })
                    .collect(Collectors.toList());
            if(chatChannelSenders.contains(null)) {
                //無効なチャンネルがあれば
                DiscordConnect.getInstance().getLogger().severe(Message.mainChannelNotFound.toString());
                DiscordConnect.getInstance().getLogger().severe(Message.shutdownDueToError.toString());
                chatChannelSenders = null;
                bot.shutdownNow();
                return;
            }
            chatChannelSenders.forEach(Thread::start);
            DiscordConnect.getInstance().getProxy().getPluginManager().registerListener(DiscordConnect.getInstance(), DiscordConnect.getInstance().getBungeeListener());
            ChatCasterListener chatCasterListener = DiscordConnect.getInstance().getChatCasterListener();
            LunaChatListener lunaChatListener = DiscordConnect.getInstance().getLunaChatListener();
            if(chatCasterListener != null) DiscordConnect.getInstance().getProxy().getPluginManager().registerListener(DiscordConnect.getInstance(), chatCasterListener);
            if(lunaChatListener != null) DiscordConnect.getInstance().getProxy().getPluginManager().registerListener(DiscordConnect.getInstance(), lunaChatListener);
            DiscordConnect.getInstance().getBotManager().updateGameName(
                    DiscordConnect.getInstance().getProxy().getPlayers().size(),
                    DiscordConnect.getInstance().getProxy().getConfig().getPlayerLimit()
            );

            DiscordConnect.getInstance().getLogger().info(Message.botIsReady.toString());

            if(isRestarting) {
                DiscordConnect.getInstance().getLogger().info(Message.botRestarted.toString());
                isRestarting = false;
                return;
            }

            sendMessageToChatChannel(
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
        }
    }

    /**
     * チャットチャンネルへメッセージを送信
     * @param mes メッセージ
     */
    public void sendMessageToChatChannel(String mes) {
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
     * チャットチャンネルのIDリストを取得
     * @return IDリスト
     */
    public List<Long> getChatChannelIds() {
        return chatChannelIds;
    }

    /**
     * プレイ中のゲーム名を更新
     * @param playerCount プレイヤー数
     * @param maxPlayers 最大プレイヤー数
     */
    public void updateGameName(int playerCount, int maxPlayers) {
        String maxPlayersString = maxPlayers != -1 ? String.valueOf(maxPlayers) : "∞";

        bot.getPresence().setActivity(
                Activity.playing(playingGameName
                        .replace("{players}", String.valueOf(playerCount))
                        .replace("{max}", maxPlayersString)));
    }
}
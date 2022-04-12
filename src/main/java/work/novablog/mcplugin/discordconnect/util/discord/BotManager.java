package work.novablog.mcplugin.discordconnect.util.discord;

import me.scarsz.jdaappender.ChannelLoggingHandler;
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
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.command.DiscordCommandExecutor;
import work.novablog.mcplugin.discordconnect.listener.DiscordListener;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;

import javax.security.auth.login.LoginException;
import java.awt.*;
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
    private ChannelLoggingHandler loggingHandler;
    private final String playingGameName;
    private final Boolean enableConsoleChannel;
    private final @Nullable Long consoleChannelId;


    private boolean isActive;
    private static boolean isRestarting = false;

    /**
     * discordのbotでメッセージを送信するためのbot管理インスタンスを生成します
     * @param token botのトークン
     * @param chatChannelIds BungeeCordのチャットと連携されるDiscordチャンネルのID
     * @param playingGameName botのステータスに表示されるプレイ中のゲーム名
     * @param prefix コマンドのprefix
     * @param toMinecraftFormat DiscordのメッセージをBungeeCordに転送するときのフォーマット
     * @param fromDiscordToDiscordName Discordのメッセージを再送するときの名前欄のフォーマット
     * @param discordCommandExecutor discordのコマンドの解析や実行を行うインスタンス
     * @param enableConsoleChannel コンソールチャンネルを有効化するか否か
     * @param consoleChannelId (有効にする場合) コンソールチャンネルのID
     * @param allowDispatchCommandFromConsoleChannel (有効にする場合) コンソールチャンネルからのコマンド実行を許可するか否か
     * @throws LoginException botのログインに失敗した場合
     */
    public BotManager(
            @NotNull String token,
            @NotNull List<Long> chatChannelIds,
            @NotNull String playingGameName,
            @NotNull String prefix,
            @NotNull String toMinecraftFormat,
            @NotNull String fromDiscordToDiscordName,
            @NotNull DiscordCommandExecutor discordCommandExecutor,
            @NotNull Boolean enableConsoleChannel,
            @Nullable Long consoleChannelId,
            @Nullable Boolean allowDispatchCommandFromConsoleChannel
    ) throws LoginException {
        //ログインする
        bot = JDABuilder.create(token, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(this)
                .setAutoReconnect(true)
                .build();
        bot.addEventListener(
                new DiscordListener(prefix, toMinecraftFormat, fromDiscordToDiscordName, discordCommandExecutor, consoleChannelId, allowDispatchCommandFromConsoleChannel)
        );

        this.chatChannelIds = chatChannelIds;
        this.playingGameName = playingGameName;
        this.enableConsoleChannel = enableConsoleChannel;
        this.consoleChannelId = consoleChannelId;
        this.isActive = false;
    }

    /**
     * botをシャットダウンします
     * @param isRestart botの再起動かどうか
     *                  trueの場合、プロキシの開始・停止メッセージが表示されません。
     */
    public void botShutdown(boolean isRestart) {
        if(!isActive) return;

        isActive = false;
        isRestarting = isRestart;
        DiscordConnect.getInstance().getLogger().info(ConfigManager.Message.normalShutdown.toString());

        if(!isRestart && chatChannelSenders != null) {
            //プロキシ終了メッセージ
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(ConfigManager.Message.serverActivity.toString());
            eb.setColor(new Color(102, 205, 170));
            eb.setDescription(ConfigManager.Message.proxyStopped.toString());
            MessageEmbed message = eb.build();
            getChatChannelSenders().forEach(sender -> sender.addQueue(message));

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

        if(loggingHandler != null) {
            loggingHandler.shutdown();
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

            // JDAAppenderを適用
            if(enableConsoleChannel) {
                // consoleChannelId が null でないことをチェックする
                if(consoleChannelId == null) {
                    // nullだった場合シャットダウンする
                    DiscordConnect.getInstance().getLogger().severe(
                            ConfigManager.Message.configPropertyIsNull.toString()
                                    .replace("{property}", "consoleChannel.consoleChannelId")
                    );
                    DiscordConnect.getInstance().getLogger().severe(ConfigManager.Message.shutdownDueToError.toString());
                    bot.shutdownNow();
                    return;
                }

                TextChannel consoleChannel = bot.getTextChannelById(consoleChannelId);

                if(consoleChannel == null) {
                    DiscordConnect.getInstance().getLogger().severe(ConfigManager.Message.consoleChannelNotFound.toString());
                    DiscordConnect.getInstance().getLogger().severe(ConfigManager.Message.shutdownDueToError.toString());
                    bot.shutdownNow();
                    return;
                }

                loggingHandler = new ChannelLoggingHandler(() -> consoleChannel, config -> {
                    config.setUseCodeBlocks(true);
                    config.setSplitCodeBlockForLinks(false);
                    config.setAllowLinkEmbeds(true);
                    config.setColored(true);
                    config.setTruncateLongItems(true);
                }).attachLog4jLogging().schedule();
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

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(ConfigManager.Message.serverActivity.toString());
            eb.setColor(new Color(102, 205, 170));
            eb.setDescription(ConfigManager.Message.proxyStarted.toString());
            MessageEmbed message = eb.build();
            getChatChannelSenders().forEach(sender -> sender.addQueue(message));
        }
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
     * チャットチャンネルをすべて返します
     * @return チャットチャンネルの送信インスタンス
     */
    public List<DiscordBotSender> getChatChannelSenders() {
        return chatChannelSenders;
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
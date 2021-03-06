package work.novablog.mcplugin.discordconnect.util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.listener.DiscordListener;

import javax.security.auth.login.LoginException;

/**
 * DiscordBotの管理を行う
 */
public class BotManager implements EventListener {
    private JDA bot;
    private long mainChannelId;
    private DiscordSender mainChannelSender;

    private boolean isActive = false;

    public BotManager(String token, long mainChannelId, String playingGameName, String prefix) {
        botLogin(token, mainChannelId, playingGameName, prefix);
    }

    /**
     * メインチャンネルへメッセージを送信
     * @param mes メッセージ
     */
    public void sendMessageToMainChannel(String mes) {
        mainChannelSender.addQueue(mes);
    }

    /**
     * botをログインする
     * @param token botのトークン
     * @param mainChannelId メインチャンネルのID
     * @param playingGameName プレイ中のゲーム名
     * @param prefix コマンドのプレフィックス
     */
    public void botLogin(String token, long mainChannelId, String playingGameName, String prefix) {
        //ログインする
        try {
            bot = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing(playingGameName))
                    .addEventListeners(this)
                    .build();
            bot.addEventListener(new DiscordListener(mainChannelId, prefix));
        } catch (LoginException e) {
            DiscordConnect.getInstance().getLogger().severe(Message.invalidToken.toString());
            isActive = false;
            return;
        }

        this.mainChannelId = mainChannelId;
        isActive = true;
    }

    /**
     * botをシャットダウンする
     */
    public void botShutdown() {
        if(isActive) {
            //メインチャンネルスレッドの停止
            DiscordConnect.getInstance().getLogger().info(Message.normalShutdown.toString());
            mainChannelSender.threadStop();
            try {
                mainChannelSender.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mainChannelSender = null;

            //botのシャットダウン
            bot.shutdown();
            bot = null;
            isActive = false;
        }
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if(event instanceof ReadyEvent) {
            //Botのログインが完了

            //メインチャンネルを探す
            TextChannel mainChannel = bot.getTextChannelById(mainChannelId);
            if(mainChannel == null) {
                //見つからなかったら
                DiscordConnect.getInstance().getLogger().severe(Message.mainChannelNotFound.toString());
                DiscordConnect.getInstance().getLogger().severe(Message.shutdownDueToError.toString());
                bot.shutdown();
                bot = null;
                isActive = false;
                return;
            }
            mainChannelSender = new DiscordSender(mainChannel);
            mainChannelSender.start();

            DiscordConnect.getInstance().getLogger().info(Message.botIsReady.toString());
        }
    }
}
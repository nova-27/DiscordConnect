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

import javax.security.auth.login.LoginException;

/**
 * DiscordBotの管理を行う
 */
public class BotManager implements EventListener {
    private JDA bot;
    private long main_channel_id;
    private DiscordSender main_channel_sender;

    private boolean is_active = false;

    public BotManager(String token, long main_channel_id, String playing_game_name) {
        botLogin(token, main_channel_id, playing_game_name);
    }

    /**
     * botをログインする
     * @param token botのトークン
     * @param main_channel_id メインチャンネルのID
     * @param playing_game_name プレイ中のゲーム名
     */
    public void botLogin(String token, long main_channel_id, String playing_game_name) {
        //ログインする
        try {
            bot = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing(playing_game_name))
                    .addEventListeners(this)
                    .build();
        } catch (LoginException e) {
            DiscordConnect.getInstance().getLogger().severe(Message.invalidToken.toString());
            is_active = false;
            return;
        }

        this.main_channel_id = main_channel_id;
        is_active = true;
    }

    /**
     * botをシャットダウンする
     */
    public void botShutdown() {
        if(is_active) {
            //メインチャンネルスレッドの停止
            DiscordConnect.getInstance().getLogger().info(Message.normalShutdown.toString());
            main_channel_sender.threadStop();
            try {
                main_channel_sender.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            main_channel_sender = null;

            //botのシャットダウン
            bot.shutdown();
            bot = null;
            is_active = false;
        }
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if(event instanceof ReadyEvent) {
            //Botのログインが完了

            //メインチャンネルを探す
            TextChannel main_channel = bot.getTextChannelById(main_channel_id);
            if(main_channel == null) {
                //見つからなかったら
                DiscordConnect.getInstance().getLogger().severe(Message.mainChannelNotFound.toString());
                DiscordConnect.getInstance().getLogger().severe(Message.shutdownDueToError.toString());
                bot.shutdown();
                bot = null;
                is_active = false;
                return;
            }
            main_channel_sender = new DiscordSender(main_channel);
            main_channel_sender.start();

            DiscordConnect.getInstance().getLogger().info(Message.botIsReady.toString());
        }
    }
}
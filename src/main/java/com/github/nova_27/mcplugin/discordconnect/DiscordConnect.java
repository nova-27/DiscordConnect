package com.github.nova_27.mcplugin.discordconnect;

import com.github.nova_27.mcplugin.discordconnect.listener.BungeeEvent;
import com.github.nova_27.mcplugin.discordconnect.listener.ChatCasterListener;
import com.github.nova_27.mcplugin.discordconnect.listener.DiscordEvent;
import com.github.nova_27.mcplugin.discordconnect.utils.DiscordSender;
import com.github.nova_27.mcplugin.discordconnect.utils.Messages;
import com.github.nova_27.mcplugin.discordconnect.utils.ThreadBungee;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterPlugin;
import com.tjplaysnow.discord.object.Bot;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;

public final class DiscordConnect extends Plugin {
    //bot関係
    private Bot bot;
    private DiscordSender main_channel;

    //その他
    private N8ChatCasterAPI chatCasterApi = null;
    private static DiscordConnect instance;

    /**
     * インスタンスを返す
     * @return インスタンス
     */
    public static DiscordConnect getInstance() {
        return instance;
    }

    /**
     * ChatCasterAPIを返す
     * @return
     */
    public N8ChatCasterAPI getChatCasterApi() {
        return chatCasterApi;
    }

    /**
     * メインチャンネルのキューを追加する
     * @param text 送信メッセージ
     */
    public void mainChannel_AddQueue(String text) {
        main_channel.add_queue(text);
    }

    /**
     * メインチャンネルのキューを追加する
     * @param embed 送信埋め込みメッセージ
     */
    public void mainChannel_AddQueue(MessageEmbed embed) {
        main_channel.add_queue(embed);
    }

    /**
     * 同期的にメッセージを送信する
     * @param channelID チャンネルID
     * @param text メッセージ
     */
    public void sendToDiscord_sync(long channelID, String text) {
        bot.getBot().getTextChannelById(channelID).sendMessage(text).complete();
    }

    /**
     * 同期的に埋め込みメッセージを送信する
     * @param channelID チャンネルID
     * @param embed 埋め込みメッセージ
     */
    public void sendToDiscord_sync(long channelID, MessageEmbed embed) {
        bot.getBot().getTextChannelById(channelID).sendMessage(embed).complete();
    }

    /**
     * プラグイン有効時に呼び出される
     */
    @Override
    public void onEnable() {
        instance = this;

        //イベントを登録
        getProxy().getPluginManager().registerListener(this, new BungeeEvent());

        //プラグイン連携 N8ChatListener
        Plugin temp = getProxy().getPluginManager().getPlugin("N8ChatCaster");
        if (temp instanceof N8ChatCasterPlugin) {
            chatCasterApi = (((N8ChatCasterPlugin) temp).getChatCasterApi());
            getProxy().getPluginManager().registerListener(this, new ChatCasterListener());
        }

        //プラグイン連携 SMFB
        /*Plugin temp2 = getProxy().getPluginManager().getPlugin("SMFBCore");
        if(temp2 instanceof Smfb_core) {
            smfb = (Smfb_core)temp2;
            ConfigData.Servers.Process
        }*/

        //設定フォルダ
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        //言語ファイル
        File language_file = new File(getDataFolder(), "message.yml");
        if (!language_file.exists()) {
            //存在しなければコピー
            InputStream src = getResourceAsStream(Locale.getDefault().toString() + ".properties");
            if(src == null) src = getResourceAsStream("ja_JP.properties");

            try {
                Files.copy(src, language_file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //configファイル
        File plugin_config = new File(getDataFolder(), "config.yml");
        if (!plugin_config.exists()) {
            //存在しなければコピー
            InputStream src = getResourceAsStream("config.yml");

            try {
                Files.copy(src, plugin_config.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        try {
            //config取得
            Configuration plugin_configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(plugin_config);
            String TOKEN = plugin_configuration.getString("Token");
            ConfigData.mainChannelID = plugin_configuration.getLong("ChannelID");
            ConfigData.prefix = plugin_configuration.getString("Prefix");

            ConfigData.toMinecraft = plugin_configuration.getString("toMinecraft");
            ConfigData.toDiscord = plugin_configuration.getString("toDiscord");
            ConfigData.playingGame = plugin_configuration.getString("PlayingGame");

            //ボット設定
            bot = new Bot(TOKEN, ConfigData.prefix);
            bot.setBotThread(new ThreadBungee(this));
            bot.setLoadAction(() -> {
                DiscordConnect.getInstance().embed(Color.GREEN, Messages.proxyStarted.toString(), null);
                main_channel.start();

                int maxPlayerInt = getProxy().getConfig().getPlayerLimit();
                String maxPlayer = maxPlayerInt != -1 ? String.valueOf(maxPlayerInt) : "∞";

                setGame(ConfigData.playingGame
                        .replace("{players}", String.valueOf(getProxy().getPlayers().size()))
                        .replace("{max}", maxPlayer)
                );
            });
            bot.addEvent(new DiscordEvent());

            //メッセージ送信クラス
            main_channel = new DiscordSender(ConfigData.mainChannelID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * プラグイン無効時に呼び出される
     */
    @Override
    public void onDisable() {
        if (bot == null) return;

        DiscordConnect.getInstance().embed(Color.RED, Messages.proxyStopped.toString(), null);

        main_channel.thread_stop();
        try {
            main_channel.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * embedメッセージを送信する
     * @param color 色
     * @param title タイトル
     * @param list フィールドリスト
     */
    public void embed(Color color, String title, String[][] list) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(color);

        eb.setTitle(title);

        if(list != null) {
            for (String[] obj : list) {
                eb.addField(obj[0], obj[1], false);
            }
        }

        mainChannel_AddQueue(eb.build());
    }

    /**
     * プレイ中のゲームを設定する
     * @param playing プレイ中のゲーム
     */
    public void setGame(String playing) {
        bot.getBot().getPresence().setGame(Game.playing(playing));
    }
}

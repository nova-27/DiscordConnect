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
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import javax.print.DocFlavor;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;

public final class DiscordConnect extends Plugin {

    private Bot bot;
    private DiscordSender main_channel;
    public static String TOKEN;
    public static long CHANNELID;
    public static String PREFIX;

    private N8ChatCasterAPI chatCasterApi = null;
    private static DiscordConnect instance;

    public static DiscordConnect getInstance() {
        return instance;
    }

    public N8ChatCasterAPI getChatCasterApi() {
        return chatCasterApi;
    }

    public void mainChannel_AddQueue(String text) {
        main_channel.add_queue(text);
    }

    public void sendToDiscord_sync(String text) {
        bot.getBot().getTextChannelById(CHANNELID).sendMessage(text).complete();
    }

    @Override
    public void onEnable() {
        instance = this;
        main_channel = new DiscordSender(CHANNELID);

        //イベントを登録
        getProxy().getPluginManager().registerListener(this, new BungeeEvent());

        //プラグイン連携 N8ChatListener
        Plugin temp = getProxy().getPluginManager().getPlugin("N8ChatCaster");
        if (temp instanceof N8ChatCasterPlugin) {
            chatCasterApi = (((N8ChatCasterPlugin) temp).getChatCasterApi());
            getProxy().getPluginManager().registerListener(this, new ChatCasterListener());
        }

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

        //config取得
        try {
            Configuration plugin_configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(plugin_config);
            TOKEN = plugin_configuration.getString("Token");
            CHANNELID = plugin_configuration.getLong("ChannelID");
            PREFIX = plugin_configuration.getString("Prefix");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //ボット設定
        bot = new Bot(TOKEN, PREFIX);
        bot.setBotThread(new ThreadBungee(this));
        bot.setLoadAction(() -> {
            DiscordConnect.getInstance().embed(Color.GREEN, Messages.proxyStarted.toString(), null);
            main_channel.start();
        });
        bot.addEvent(new DiscordEvent());
    }

    @Override
    public void onDisable() {
        if (bot == null) return;

        main_channel.thread_stop();
        try {
            main_channel.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DiscordConnect.getInstance().embed(Color.RED, Messages.proxyStopped.toString(), null);
    }

    public void embed(Color color, String title, String[][] list) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(color);

        eb.setTitle(title);

        if(list != null) {
            for (String[] obj : list) {
                eb.addField(obj[0], obj[1], false);
            }
        }

        bot.getBot().getTextChannelById(CHANNELID).sendMessage(eb.build()).complete();
    }
}

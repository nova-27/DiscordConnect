package work.novablog.mcplugin.discordconnect;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatBungee;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterPlugin;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;
import work.novablog.mcplugin.discordconnect.command.bungee.BungeeCommand;
import work.novablog.mcplugin.discordconnect.listener.BungeeListener;
import work.novablog.mcplugin.discordconnect.listener.BungeeLoggingListener;
import work.novablog.mcplugin.discordconnect.listener.ChatCasterListener;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;
import work.novablog.mcplugin.discordconnect.util.BotManager;
import work.novablog.mcplugin.discordconnect.util.GithubAPI;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;
import java.util.logging.Level;

public final class DiscordConnect extends Plugin {
    private static final int CONFIG_LATEST = 5;
    private static final String PLUGIN_DOWNLOAD_LINK = "https://github.com/nova-27/DiscordConnect/releases";

    private static DiscordConnect instance;
    private N8ChatCasterAPI chatCasterAPI;
    private LunaChatAPI lunaChatAPI;
    private Properties langData;

    private BotManager botManager;
    private BungeeListener bungeeListener;
    private BungeeLoggingListener bungeeLoggingListener;
    private LunaChatListener lunaChatListener;
    private ChatCasterListener chatCasterListener;

    /**
     * インスタンスを返す
     *
     * @return インスタンス
     */
    public static DiscordConnect getInstance() {
        return instance;
    }

    /**
     * 言語データを返す
     *
     * @return 言語データ
     */
    public Properties getLangData() {
        return langData;
    }

    /**
     * configを読み直してbotを再起動する
     */
    public void reload() {
        botManager.sendMessageToChannel(
                BotManager.ChannelType.ALL,
                Message.serverActivity.toString(),
                null,
                Message.botRestarting.toString(),
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

        shutdown();
        setup();
    }

    @Override
    public void onEnable() {
        instance = this;

        //bstats
        new Metrics(this, 7990);

        //N8ChatCasterと連携
        Plugin temp = getProxy().getPluginManager().getPlugin("N8ChatCaster");
        if (temp instanceof N8ChatCasterPlugin) {
            chatCasterAPI = (((N8ChatCasterPlugin) temp).getChatCasterApi());
        }

        //LunaChatと連携
        temp = getProxy().getPluginManager().getPlugin("LunaChat");
        if (temp instanceof LunaChatBungee) {
            lunaChatAPI = ((LunaChatBungee) temp).getLunaChatAPI();
        }

        setup();

        //コマンドの追加
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand());
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    private void setup() {
        Configuration config;
        try {
            config = loadConfig();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Exception", e);
            return;
        }

        String token = config.getString("token");
        List<Long> chatChannelIds = config.getLongList("chatChannelIDs");
        String playingGameName = config.getString("playingGameName");
        String toMinecraftFormat = config.getString("toMinecraftFormat");
        String toDiscordFormat = config.getString("toDiscordFormat");
        List<String> hiddenServers = config.getStringList("hiddenServers");
        long consoleChannelId = config.getLong("consoleChannel.channelId");
        List<Long> consoleChannelIds = consoleChannelId == -1 ?
                Collections.emptyList() :
                Collections.singletonList(consoleChannelId);
        String consoleChannelLogFormat = config.getString("consoleChannel.logFormat");

        botManager = new BotManager(
                getLogger(),
                token,
                chatChannelIds,
                consoleChannelIds,
                playingGameName,
                toMinecraftFormat
        );
        bungeeListener = new BungeeListener(botManager, toDiscordFormat, hiddenServers);
        getProxy().getPluginManager().registerListener(this, bungeeListener);
        if (!consoleChannelIds.isEmpty()) {
            bungeeLoggingListener = new BungeeLoggingListener(botManager, consoleChannelLogFormat);
            ProxyServer.getInstance().getLogger().addHandler(bungeeLoggingListener);
        }
        if (lunaChatAPI != null) {
            lunaChatListener = new LunaChatListener(botManager, toDiscordFormat);
            getProxy().getPluginManager().registerListener(this, lunaChatListener);
        }
        if (chatCasterAPI != null) {
            chatCasterListener = new ChatCasterListener(botManager, chatCasterAPI);
            getProxy().getPluginManager().registerListener(this, chatCasterListener);
        }

        // アップデートチェック
        boolean updateCheck = config.getBoolean("updateCheck");
        String currentVer = getDescription().getVersion();
        String latestVer = GithubAPI.getLatestVersionNum();
        if (updateCheck) {
            if (latestVer == null) {
                // チェックに失敗
                getLogger().info(
                        Message.updateCheckFailed.toString()
                );
            } else if (currentVer.equals(latestVer)) {
                // すでに最新
                getLogger().info(
                        Message.pluginIsLatest.toString()
                                .replace("{current}", currentVer)
                );
            } else {
                // 新しいバージョンがある
                getLogger().info(
                        Message.updateNotice.toString()
                                .replace("{current}", currentVer)
                                .replace("{latest}", latestVer)
                );
                getLogger().info(
                        Message.updateDownloadLink.toString()
                                .replace("{link}", PLUGIN_DOWNLOAD_LINK)
                );
            }
        }
    }

    private void shutdown() {
        if (bungeeListener != null) getProxy().getPluginManager().unregisterListener(bungeeListener);
        if (bungeeLoggingListener != null) ProxyServer.getInstance().getLogger().removeHandler(bungeeLoggingListener);
        if (lunaChatListener != null) getProxy().getPluginManager().unregisterListener(lunaChatListener);
        if (chatCasterListener != null) getProxy().getPluginManager().unregisterListener(chatCasterListener);
        botManager.botShutdown();
        botManager = null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Configuration loadConfig() throws IOException {
        //設定フォルダ
        getDataFolder().mkdir();

        //言語ファイル
        File langFile = new File(getDataFolder(), "message.yml");
        if (!langFile.exists()) {
            //存在しなければコピー
            InputStream src = getResourceAsStream(Locale.getDefault().toString() + ".properties");
            if (src == null) src = getResourceAsStream("ja_JP.properties");
            Files.copy(src, langFile.toPath());
        }

        langData = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(langFile.toPath()),
                StandardCharsets.UTF_8)
        ) {
            langData.load(reader);
        }

        //configファイル
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            //存在しなければコピー
            InputStream src = getResourceAsStream("config.yml");
            Files.copy(src, configFile.toPath());
        }

        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

        //configが古ければ新しいconfigをコピー
        int configVersion = config.getInt("configVersion", 0);
        if (configVersion < CONFIG_LATEST) {
            //古いlangをバックアップ
            File oldLangFile = new File(getDataFolder(), "message_old.yml");
            Files.move(langFile.toPath(), oldLangFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            //新しいlangファイルをコピー
            langFile = new File(getDataFolder(), "message.yml");
            InputStream src = getResourceAsStream(Locale.getDefault().toString() + ".properties");
            if (src == null) src = getResourceAsStream("ja_JP.properties");
            Files.copy(src, langFile.toPath());
            try (InputStreamReader reader = new InputStreamReader(
                    Files.newInputStream(langFile.toPath()),
                    StandardCharsets.UTF_8)
            ) {
                langData.load(reader);
            }

            //古いconfigをバックアップ
            File oldConfigFile = new File(getDataFolder(), "config_old.yml");
            Files.move(configFile.toPath(), oldConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            //新しいconfigをコピー
            src = getResourceAsStream("config.yml");
            Files.copy(src, configFile.toPath());
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            getLogger().info(Message.configIsOld.toString());
        }

        return config;
    }
}

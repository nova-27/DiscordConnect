package work.novablog.mcplugin.discordconnect;

import com.github.ucchyocean.lc3.LunaChatBungee;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterPlugin;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;
import work.novablog.mcplugin.discordconnect.command.BungeeMinecraftCommand;
import work.novablog.mcplugin.discordconnect.listener.BungeeListener;
import work.novablog.mcplugin.discordconnect.listener.ChatCasterListener;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;
import work.novablog.mcplugin.discordconnect.util.BotManager;
import work.novablog.mcplugin.discordconnect.util.GithubAPI;
import work.novablog.mcplugin.discordconnect.util.Message;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class DiscordConnect extends Plugin {
    private static final int CONFIG_LATEST = 5;
    private static final String pluginDownloadLink = "https://github.com/nova-27/DiscordConnect/releases";

    private static DiscordConnect instance;
    private BotManager botManager;
    private Configuration langData;
    private BungeeListener bungeeListener;

    private N8ChatCasterAPI chatCasterAPI;
    private ChatCasterListener chatCasterListener;

    private LunaChatBungee lunaChat;
    private LunaChatListener lunaChatListener;

    /**
     * インスタンスを返す
     * @return インスタンス
     */
    public static DiscordConnect getInstance() {
        return instance;
    }

    /**
     * Botマネージャーを返す
     * @return botマネージャー
     */
    public BotManager getBotManager() {
        return botManager;
    }

    /**
     * 言語データを返す
     * @return 言語データ
     */
    public Configuration getLangData() {
        return langData;
    }

    /**
     * BungeeListenerを返す
     * @return BungeeListener
     */
    public BungeeListener getBungeeListener() {
        return bungeeListener;
    }

    /**
     * ChatCasterAPIを返す
     * @return chatCasterAPI
     */
    public @Nullable N8ChatCasterAPI getChatCasterAPI() {
        return chatCasterAPI;
    }

    /**
     * ChatCasterListenerを返す
     * @return chatCasterListener
     */
    public ChatCasterListener getChatCasterListener() {
        return chatCasterListener;
    }

    /**
     * LunaChatを返す
     * @return lunaChat
     */
    public LunaChatBungee getLunaChat() {
        return lunaChat;
    }

    /**
     * LunaChatListenerを返す
     * @return lunaChatListener
     */
    public LunaChatListener getLunaChatListener() {
        return lunaChatListener;
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
            chatCasterListener = new ChatCasterListener();
        }

        //LunaChatと連携
        temp = getProxy().getPluginManager().getPlugin("LunaChat");
        if(temp instanceof LunaChatBungee) {
            lunaChat = (LunaChatBungee) temp;
            lunaChatListener = new LunaChatListener();
        }

        //configの読み込み
        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //コマンドの追加
        getProxy().getPluginManager().registerCommand(this, new BungeeMinecraftCommand());
    }

    public void loadConfig() throws IOException {
        if(botManager != null) {
            botManager.botShutdown(true);
            botManager = null;
        }

        //設定フォルダ
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        //configファイル
        File pluginConfig = new File(getDataFolder(), "config.yml");
        InputStream pluginConfigResource = getResourceAsStream("config.yml");
        if (!pluginConfig.exists()) {
            //存在しなければコピー
            Files.copy(pluginConfigResource, pluginConfig.toPath());
        }

        //config取得
        Configuration pluginConfiguration =
                ConfigurationProvider.getProvider(YamlConfiguration.class).load(pluginConfig);

        //言語ファイル
        File languageFile = new File(getDataFolder(), "message.yml");
        InputStream languageResource = getResourceAsStream(Locale.getDefault().toString() + ".yml");
        if(languageResource == null) languageResource = getResourceAsStream("ja_JP.yml");
        if (!languageFile.exists()) {
            //存在しなければコピー
            Files.copy(languageResource, languageFile.toPath());
        }

        int configVersion = pluginConfiguration.getInt("configVersion", 0);
        //configが古ければ新しいconfigをコピー
        if(configVersion < CONFIG_LATEST) {
            //古いconfigをリネーム
            Files.move(
                    pluginConfig.toPath(),
                    Paths.get(getDataFolder() + File.separator + "config_old.yml"),
                    REPLACE_EXISTING
            );

            //新しいconfigをコピー
            Files.copy(pluginConfigResource, pluginConfig.toPath());
            pluginConfiguration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(pluginConfig);

            //古いlangファイルをリネーム
            Files.move(
                    languageFile.toPath(),
                    Paths.get(getDataFolder() + File.separator + "message_old.yml"),
                    REPLACE_EXISTING
            );

            //新しいlangファイルをコピー
            Files.copy(languageResource, languageFile.toPath());
            langData = ConfigurationProvider.getProvider(YamlConfiguration.class).load(languageFile);
            getLogger().info(Message.configIsOld.toString());
        }

        //Messageの準備
        langData = ConfigurationProvider.getProvider(YamlConfiguration.class).load(languageFile);

        String token = pluginConfiguration.getString("token");
        List<Long> chatChannelIds = pluginConfiguration.getLongList("chatChannelIDs");
        String playingGameName = pluginConfiguration.getString("playingGameName");
        String prefix = pluginConfiguration.getString("prefix");
        String toMinecraftFormat = pluginConfiguration.getString("toMinecraftFormat");
        String toDiscordFormat = pluginConfiguration.getString("toDiscordFormat");
        String japanizeFormat = pluginConfiguration.getString("japanizeFormat");
        bungeeListener = new BungeeListener(toDiscordFormat);
        if(lunaChatListener != null) {
            lunaChatListener.setToDiscordFormat(toDiscordFormat);
            lunaChatListener.setJapanizeFormat(japanizeFormat);
        }
        botManager = new BotManager(token, chatChannelIds, playingGameName, prefix, toMinecraftFormat);

        // アップデートチェック
        boolean updateCheck = pluginConfiguration.getBoolean("updateCheck");
        String currentVer = getDescription().getVersion();
        String latestVer = GithubAPI.getLatestVersionNum();
        if(updateCheck) {
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
            }else{
                // 新しいバージョンがある
                getLogger().info(
                        Message.updateNotice.toString()
                                .replace("{current}", currentVer)
                                .replace("{latest}", latestVer)
                );
                getLogger().info(
                        Message.updateDownloadLink.toString()
                                .replace("{link}",pluginDownloadLink)
                );
            }
        }
    }

    @Override
    public void onDisable() {
        botManager.botShutdown(false);
    }
}

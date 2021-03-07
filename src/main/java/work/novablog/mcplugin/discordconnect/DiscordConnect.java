package work.novablog.mcplugin.discordconnect;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import work.novablog.mcplugin.discordconnect.command.BungeeMinecraftCommand;
import work.novablog.mcplugin.discordconnect.util.BotManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public final class DiscordConnect extends Plugin {
    private static DiscordConnect instance;
    private BotManager botManager;
    private Properties langData;

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
    public Properties getLangData() {
        return langData;
    }

    @Override
    public void onEnable() {
        instance = this;

        //configの読み込み
        loadConfig();

        //コマンドの追加
        getProxy().getPluginManager().registerCommand(this, new BungeeMinecraftCommand());
    }

    public void loadConfig() {
        if(botManager != null) {
            botManager.botShutdown();
            botManager = null;
        }

        //設定フォルダ
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        //言語ファイル
        File languageFile = new File(getDataFolder(), "message.yml");
        if (!languageFile.exists()) {
            //存在しなければコピー
            InputStream src = getResourceAsStream(Locale.getDefault().toString() + ".properties");
            if(src == null) src = getResourceAsStream("ja_JP.properties");

            try {
                Files.copy(src, languageFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Messageの準備
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(languageFile), StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(Objects.requireNonNull(inputStreamReader));
            langData = new Properties();
            langData.load(bufferedReader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //configファイル
        File pluginConfig = new File(getDataFolder(), "config.yml");
        if (!pluginConfig.exists()) {
            //存在しなければコピー
            InputStream src = getResourceAsStream("config.yml");

            try {
                Files.copy(src, pluginConfig.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //config取得・bot起動
        Configuration pluginConfiguration = null;
        try {
            pluginConfiguration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(pluginConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String token = pluginConfiguration.getString("token");
        List<Long> chatChannelIds = pluginConfiguration.getLongList("chatChannelIDs");
        String playingGameName = pluginConfiguration.getString("playingGameName");
        String prefix = pluginConfiguration.getString("prefix");
        String toMinecraftFormat = pluginConfiguration.getString("toMinecraftFormat");
        botManager = new BotManager(token, chatChannelIds, playingGameName, prefix, toMinecraftFormat);
    }

    @Override
    public void onDisable() {
        botManager.botShutdown();
    }
}

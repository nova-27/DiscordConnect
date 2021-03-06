package work.novablog.mcplugin.discordconnect;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import work.novablog.mcplugin.discordconnect.command.BungeeMinecraftCommand;
import work.novablog.mcplugin.discordconnect.util.BotManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;

public final class DiscordConnect extends Plugin {
    private static DiscordConnect instance;
    private BotManager botManager;
    private PropertyResourceBundle messages;

    /**
     * インスタンスを返す
     * @return インスタンス
     */
    public static DiscordConnect getInstance() {
        return instance;
    }

    /**
     * 言語データを返す
     * @return 言語データ
     */
    public PropertyResourceBundle getLangFile() {
        return messages;
    }

    /**
     * Botマネージャーを返す
     * @return botマネージャー
     */
    public BotManager getBotManager() {
        return botManager;
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

        //Messageの準備
        try {
            File message_file = new File(getDataFolder(), "message.yml");
            InputStreamReader fileReader = new InputStreamReader(new FileInputStream(message_file), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(Objects.requireNonNull(fileReader));
            messages = new PropertyResourceBundle(reader);
        } catch (IOException e) {
            e.printStackTrace();
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
        }

        //config取得・bot起動
        Configuration plugin_configuration = null;
        try {
            plugin_configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(plugin_config);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String token = plugin_configuration.getString("token");
        long main_channel_id = plugin_configuration.getLong("mainChannelID");
        String playing_game_name = plugin_configuration.getString("playingGameName");
        String prefix = plugin_configuration.getString("prefix");
        botManager = new BotManager(token, main_channel_id, playing_game_name, prefix);
    }

    @Override
    public void onDisable() {
        botManager.botShutdown();
    }
}

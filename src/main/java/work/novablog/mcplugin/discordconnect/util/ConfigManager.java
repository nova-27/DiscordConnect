package work.novablog.mcplugin.discordconnect.util;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

public class ConfigManager {
    private static final int CONFIG_LATEST = 3;

    private static Configuration langData;

    public String botToken;
    public List<String> botWebhookURLs;
    public List<Long> botChatChannelIds;
    public String botPlayingGameName;

    public String botCommandPrefix;
    public boolean doUpdateCheck;

    public String fromDiscordToMinecraftFormat;
    public String fromMinecraftToDiscordName;
    public String fromDiscordToDiscordName;

    public String lunaChatJapanizeFormat;

    /**
     * configの読み出し、保持を行うインスタンスを生成します
     * @param plugin プラグインのメインクラス
     * @throws IOException 読み出し中にエラーが発生した場合にthrowされます
     */
    public ConfigManager(@NotNull Plugin plugin) throws IOException {
        //設定フォルダの作成
        if(!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdir()) {
            throw new IOException();
        }

        //バージョンが古ければ古いファイルをバックアップ
        if (getConfigData(plugin).getInt("configVersion", 0) < CONFIG_LATEST) {
            backupOldFile(plugin, "config.yml");
            backupOldFile(plugin, "message.yml");
        }

        //configとlangの取得
        Configuration pluginConfig = getConfigData(plugin);
        langData = getLangData(plugin);

        //configの読み出し
        botToken = pluginConfig.getString("token");
        botWebhookURLs = pluginConfig.getStringList("webhookURLs");
        botChatChannelIds = pluginConfig.getLongList("chatChannelIDs");
        botPlayingGameName = pluginConfig.getString("playingGameName");

        botCommandPrefix = pluginConfig.getString("prefix");
        doUpdateCheck = pluginConfig.getBoolean("updateCheck");

        fromDiscordToMinecraftFormat = pluginConfig.getString("fromDiscordToMinecraftFormat");
        fromMinecraftToDiscordName = pluginConfig.getString("fromMinecraftToDiscordName");
        fromDiscordToDiscordName = pluginConfig.getString("fromDiscordToDiscordName");

        lunaChatJapanizeFormat = pluginConfig.getString("japanizeFormat");
    }

    private Configuration getConfigData(Plugin plugin) throws IOException {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            //存在しなければコピー
            InputStream src = plugin.getResourceAsStream("config.yml");
            Files.copy(src, configFile.toPath());
        }

        return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
    }

    private Configuration getLangData(Plugin plugin) throws IOException {
        File langFile = new File(plugin.getDataFolder(), "message.yml");
        if (!langFile.exists()) {
            //存在しなければコピー
            InputStream src = plugin.getResourceAsStream(Locale.getDefault().toString() + ".yml");
            if(src == null) src = plugin.getResourceAsStream("ja_JP.yml");
            Files.copy(src, langFile.toPath());
        }

        return ConfigurationProvider.getProvider(YamlConfiguration.class).load(langFile);
    }

    private void backupOldFile(Plugin plugin, String targetFileName) throws IOException {
        File oldFile = new File(plugin.getDataFolder(), targetFileName + "_old");
        Files.deleteIfExists(oldFile.toPath());
        if(!(new File(plugin.getDataFolder(), targetFileName).renameTo(oldFile))) throw new IOException();
    }

    /**
     * 多言語対応メッセージ
     */
    public enum Message {
        invalidToken,
        invalidWebhookURL,
        mainChannelNotFound,
        shutdownDueToError,
        normalShutdown,
        botIsReady,
        botRestarted,
        configReloaded,

        updateNotice,
        updateDownloadLink,
        updateCheckFailed,
        pluginIsLatest,

        bungeeCommandDenied,
        bungeeCommandNotFound,
        bungeeCommandSyntaxError,

        bungeeCommandHelpLine1,
        bungeeCommandHelpHelpcmd,
        bungeeCommandHelpReloadcmd,

        userActivity,
        serverActivity,

        proxyStarted,
        proxyStopped,
        joined,
        left,
        serverSwitched;

        /**
         * yamlファイルからメッセージを取ってきます
         * @return 多言語対応メッセージ
         */
        @Override
        public String toString() {
            return langData.getString(name());
        }
    }
}

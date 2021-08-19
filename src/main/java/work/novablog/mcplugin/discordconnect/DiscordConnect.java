package work.novablog.mcplugin.discordconnect;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatBungee;
import com.github.ucchyocean.lc3.UUIDCacheData;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterPlugin;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.command.BungeeMinecraftCommand;
import work.novablog.mcplugin.discordconnect.listener.BungeeListener;
import work.novablog.mcplugin.discordconnect.listener.ChatCasterListener;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;
import work.novablog.mcplugin.discordconnect.util.GithubAPI;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordWebhookSender;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.ArrayList;

public final class DiscordConnect extends Plugin {
    private static final String pluginDownloadLink = "https://github.com/nova-27/DiscordConnect/releases";

    private static DiscordConnect instance;
    private BotManager botManager;
    private ArrayList<DiscordWebhookSender> discordWebhookSenders;
    private BungeeListener bungeeListener;

    private N8ChatCasterAPI chatCasterAPI;
    private ChatCasterListener chatCasterListener;

    private LunaChatAPI lunaChatAPI;
    private UUIDCacheData uuidCacheData;
    private LunaChatListener lunaChatListener;

    /**
     * インスタンスを返します
     * @return インスタンス
     */
    public static DiscordConnect getInstance() {
        return instance;
    }

    /**
     * Botマネージャーを返します
     * @return botマネージャー
     */
    public @Nullable BotManager getBotManager() {
        return botManager;
    }

    /**
     * Webhook送信インスタンスの配列を返します
     * @return webhook送信インスタンス
     */
    public @NotNull
    ArrayList<DiscordWebhookSender> getDiscordWebhookSenders() {
        return discordWebhookSenders;
    }

    /**
     * ChatCasterAPIを返します
     * @return chatCasterAPI
     */
    public @Nullable N8ChatCasterAPI getChatCasterAPI() {
        return chatCasterAPI;
    }

    /**
     * LunaChatAPIを返します
     * @return lunaChatAPI
     */
    public @Nullable LunaChatAPI getLunaChatAPI() {
        return lunaChatAPI;
    }

    /**
     * botが使用可能か返します
     * @return trueの場合使用可能
     */
    public boolean canBotBeUsed() {
        return botManager != null && botManager.isActive();
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
        if(temp instanceof LunaChatBungee) {
            uuidCacheData = ((LunaChatBungee) temp).getUUIDCacheData();
            lunaChatAPI = ((LunaChatBungee) temp).getLunaChatAPI();
        }

        //コマンドの追加
        getProxy().getPluginManager().registerCommand(this, new BungeeMinecraftCommand());

        init();
    }

    /**
     * プラグインの初期設定をします
     * <p>
     *     複数回呼び出し可能です
     *     複数回呼び出した場合、新しいconfigデータが読み出されます
     * </p>
     */
    public void init() {
        if(botManager != null) botManager.botShutdown(true);
        if(discordWebhookSenders != null) discordWebhookSenders.forEach(DiscordWebhookSender::shutdown);
        if(bungeeListener != null) getProxy().getPluginManager().unregisterListener(bungeeListener);
        if(lunaChatListener != null) getProxy().getPluginManager().unregisterListener(lunaChatListener);
        if(chatCasterListener != null) getProxy().getPluginManager().unregisterListener(chatCasterListener);

        ConfigManager configManager;
        try {
            configManager = new ConfigManager(this);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            botManager = new BotManager(
                    configManager.botToken,
                    configManager.botChatChannelIds,
                    configManager.botPlayingGameName,
                    configManager.botCommandPrefix,
                    configManager.fromDiscordToMinecraftFormat,
                    configManager.fromDiscordToDiscordName
            );
        } catch (LoginException e) {
            getLogger().severe(ConfigManager.Message.invalidToken.toString());
        }

        discordWebhookSenders = new ArrayList<>();
        try {
            configManager.botWebhookURLs.forEach(url ->
                    discordWebhookSenders.add(new DiscordWebhookSender(url))
            );
        } catch(IllegalArgumentException e) {
            getLogger().severe(ConfigManager.Message.invalidWebhookURL.toString());
        }

        //BungeecordイベントのListenerを登録
        bungeeListener = new BungeeListener(configManager.fromMinecraftToDiscordName);
        getProxy().getPluginManager().registerListener(this, bungeeListener);
        if(lunaChatAPI != null) {
            lunaChatListener = new LunaChatListener(configManager.fromMinecraftToDiscordName, configManager.lunaChatJapanizeFormat, uuidCacheData);
            getProxy().getPluginManager().registerListener(this, lunaChatListener);
        }
        if(chatCasterAPI != null) {
            chatCasterListener = new ChatCasterListener(configManager.fromMinecraftToDiscordName);
            getProxy().getPluginManager().registerListener(this, chatCasterListener);
        }

        //アップデートのチェック
        if(configManager.doUpdateCheck) {
            ConfigManager.Message updateStatus;

            String latestVer = GithubAPI.getLatestVersionNum();
            String currentVer = getDescription().getVersion();
            if (latestVer.isEmpty()) {
                updateStatus = ConfigManager.Message.updateCheckFailed;
            } else if (currentVer.equals(latestVer)) {
                updateStatus = ConfigManager.Message.pluginIsLatest;
            }else{
                updateStatus = ConfigManager.Message.updateNotice;
            }

            getLogger().info(
                    updateStatus.toString()
                            .replace("{current}", currentVer)
                            .replace("{latest}", latestVer)
            );
            if(updateStatus == ConfigManager.Message.updateNotice) {
                getLogger().info(
                        ConfigManager.Message.updateDownloadLink.toString()
                                .replace("{link}", pluginDownloadLink)
                );
            }
        }
    }

    @Override
    public void onDisable() {
        if(botManager != null) botManager.botShutdown(false);
        if(discordWebhookSenders != null) discordWebhookSenders.forEach(DiscordWebhookSender::shutdown);
    }
}

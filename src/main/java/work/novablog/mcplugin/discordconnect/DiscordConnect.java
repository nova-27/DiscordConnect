package work.novablog.mcplugin.discordconnect;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatBungee;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterPlugin;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import work.novablog.mcplugin.discordconnect.command.BungeeMinecraftCommand;
import work.novablog.mcplugin.discordconnect.listener.BungeeListener;
import work.novablog.mcplugin.discordconnect.listener.ChatCasterListener;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;
import work.novablog.mcplugin.discordconnect.util.GithubAPI;
import work.novablog.mcplugin.discordconnect.util.discord.WebhookManager;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.*;

public final class DiscordConnect extends Plugin {
    private static final String pluginDownloadLink = "https://github.com/nova-27/DiscordConnect/releases";

    private static DiscordConnect instance;
    private BotManager botManager;
    private WebhookManager webhookManager;
    private BungeeListener bungeeListener;

    private N8ChatCasterAPI chatCasterAPI;
    private ChatCasterListener chatCasterListener;

    private LunaChatAPI lunaChatAPI;
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
        if(webhookManager != null) webhookManager.shutdown();
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
                    configManager.sendToMinecraftFormat
            );
        } catch (LoginException e) {
            getLogger().severe(ConfigManager.Message.invalidToken.toString());
        }

        try {
            webhookManager = new WebhookManager(
                    configManager.botWebhookURL
            );
        } catch(IllegalArgumentException e) {
            getLogger().severe(ConfigManager.Message.invalidWebhookURL.toString());
        }

        //BungeecordイベントのListenerを登録
        bungeeListener = new BungeeListener(configManager.sendToDiscordFormat);
        getProxy().getPluginManager().registerListener(this, bungeeListener);
        if(lunaChatAPI != null) {
            lunaChatListener = new LunaChatListener(configManager.sendToDiscordFormat, configManager.lunaChatJapanizeFormat);
            getProxy().getPluginManager().registerListener(this, lunaChatListener);
        }
        if(chatCasterAPI != null) {
            chatCasterListener = new ChatCasterListener();
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
        if(webhookManager != null) webhookManager.shutdown();
    }
}

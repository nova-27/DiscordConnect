package work.novablog.mcplugin.discordconnect.util;

import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;

/**
 * 多言語対応メッセージ
 */
public enum Message {
    invalidToken,
    channelNotFound,
    normalShutdown,
    botIsReady,
    botRestarting,
    configReloaded,
    configIsOld,

    updateNotice,
    updateDownloadLink,
    updateCheckFailed,
    pluginIsLatest,

    dispatchCommand,
    bungeeCommandDenied,
    bungeeCommandNotFound,

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
     * propertiesファイルからメッセージを取ってくる
     *
     * @return メッセージ
     */
    @Override
    public @NotNull String toString() {
        return DiscordConnect.getInstance().getLangData().getProperty(
                name(),
                "Message not found: " + name()
        );
    }
}

package work.novablog.mcplugin.discordconnect.util;

import work.novablog.mcplugin.discordconnect.DiscordConnect;

/**
 * 多言語対応メッセージ
 */
public enum Message {
    invalidToken,
    mainChannelNotFound,
    shutdownDueToError,
    normalShutdown,
    botIsReady,
    botRestarted,
    configReloaded,
    configIsOld,

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
    command,

    proxyStarted,
    proxyStopped,
    joined,
    left,
    serverSwitched,

    discordCommandDenied,
    discordOnlyFromDM,
    discordOnlyFromGuild;

    /**
     * yamlファイルからメッセージを取ってくる
     * @return メッセージ
     */
    @Override
    public String toString() {
        return DiscordConnect.getInstance().getLangData().getString(name());
    }
}

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
     * propertiesファイルからメッセージを取ってくる
     * @return メッセージ
     */
    @Override
    public String toString() {
        return DiscordConnect.getInstance().getLangData().getProperty(name());
    }
}

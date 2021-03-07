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
    configReloaded,

    bungeeCommandDenied,
    bungeeCommandNotFound,
    bungeeCommandSyntaxError,

    bungeeCommandHelpLine1,
    bungeeCommandHelpHelpcmd,
    bungeeCommandHelpReloadcmd,
    userActivity,

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

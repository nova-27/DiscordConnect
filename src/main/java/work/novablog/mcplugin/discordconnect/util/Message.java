package work.novablog.mcplugin.discordconnect.util;

import work.novablog.mcplugin.discordconnect.DiscordConnect;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.PropertyResourceBundle;

/**
 * 多言語対応メッセージ
 */
public enum Message {
    invalidToken,
    mainChannelNotFound,
    shutdownDueToError,
    normalShutdown,
    botIsReady,

    bungeeCommand_denied,
    bungeeCommand_notfound,
    bungeeCommand_syntaxerror,
    
    bungeeCommand_help_line1,
    bungeeCommand_help_helpcmd,
    bungeeCommand_help_reloadcmd,

    configReloaded;

    /**
     * propertiesファイルからメッセージを取ってくる
     * @return メッセージ
     */
    @Override
    public String toString() {
        return DiscordConnect.getInstance().getLangFile().getString(name());
    }
}

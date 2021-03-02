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
    bungeeCommand_help_reloadcmd;

    /**
     * propertiesファイルからメッセージを取ってくる
     * @return メッセージ
     */
    @Override
    public String toString() {
        try {
            File message_file = new File(DiscordConnect.getInstance().getDataFolder(), "message.yml");
            InputStreamReader fileReader = new InputStreamReader(new FileInputStream(message_file), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(fileReader);

            return new PropertyResourceBundle(reader).getString(name());
        }catch (IOException e) {
            return "";
        }
    }
}

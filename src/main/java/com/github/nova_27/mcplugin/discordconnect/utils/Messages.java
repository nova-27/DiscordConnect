package com.github.nova_27.mcplugin.discordconnect.utils;

import com.github.nova_27.mcplugin.discordconnect.DiscordConnect;

import java.io.*;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;

/**
 * 多言語対応メッセージ
 */
public enum Messages {
    proxyStarted,
    proxyStopped,
    toMinecraft,
    toDiscord,
    joined,
    left;

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
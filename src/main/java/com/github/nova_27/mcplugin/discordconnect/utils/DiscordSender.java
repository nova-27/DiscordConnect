package com.github.nova_27.mcplugin.discordconnect.utils;

import com.github.nova_27.mcplugin.discordconnect.DiscordConnect;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discordのチャンネルへメッセージを送信する(同期型)
 */
public class DiscordSender extends Thread {
    private final int QUEUE_BUF = 50;

    private long ChannelId;
    private int start_write = 0;
    private int start_read = 0;
    private Object[] queue = new Object[QUEUE_BUF];

    private boolean stopped = false;

    /**
     * コンストラクタ
     * @param ChannelId メッセージを送信するチャンネルID
     */
    public DiscordSender(long ChannelId) {
        this.ChannelId = ChannelId;
    }

    /**
     * キューに送信するメッセージを追加する
     * @param text 送信するメッセージ
     */
    public void add_queue(String text) {
        if(text.equals("")) return;

        queue[start_write] = text;

        start_write++;
        if (start_write >= QUEUE_BUF) {
            start_write = 0;
        }
    }

    /**
     * キューに送信する埋め込みメッセージを追加する
     * @param embed 送信する埋め込みメッセージ
     */
    public void add_queue(MessageEmbed embed) {
        queue[start_write] = embed;

        start_write++;
        if (start_write >= QUEUE_BUF) {
            start_write = 0;
        }
    }

    /**
     * スレッドを停止する
     */
    public void thread_stop() {
        stopped = true;
    }

    @Override
    public void run() {
        while (!stopped || queue[start_read] != null) {
            String Messages = "";

            //キューを読む（文字）
            while (queue[start_read] != null && queue[start_read] instanceof String) {
                Messages += queue[start_read] + "\n";
                queue[start_read] = null;

                start_read++;
                if (start_read >= QUEUE_BUF) {
                    start_read = 0;
                }
            }

            if(!Messages.equals("")) {
                //制限が2000文字なので1900文字で区切る
                Matcher m = Pattern.compile("[\\s\\S]{1,1900}").matcher(Messages);
                while (m.find()) {
                    DiscordConnect.getInstance().sendToDiscord_sync(ChannelId, m.group());
                }
            }

            //キューを読む（埋め込み）
            while (queue[start_read] != null && queue[start_read] instanceof MessageEmbed) {
                DiscordConnect.getInstance().sendToDiscord_sync(ChannelId, (MessageEmbed) queue[start_read]);
                queue[start_read] = null;

                start_read++;
                if (start_read >= QUEUE_BUF) {
                    start_read = 0;
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
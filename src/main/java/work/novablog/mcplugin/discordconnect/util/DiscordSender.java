package work.novablog.mcplugin.discordconnect.util;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordSender extends Thread{
    private final int QUEUE_BUF = 50;

    private TextChannel channel;
    private int start_write = 0;
    private int start_read = 0;
    private Object[] queue = new Object[QUEUE_BUF];

    private boolean is_stopped = false;

    public DiscordSender(TextChannel channel) {
        this.channel = channel;
    }

    /**
     * キューに送信するメッセージを追加する
     * @param text 送信するメッセージ
     */
    public void addQueue(String text) {
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
    public void addQueue(MessageEmbed embed) {
        queue[start_write] = embed;

        start_write++;
        if (start_write >= QUEUE_BUF) {
            start_write = 0;
        }
    }

    /**
     * スレッドを停止する
     */
    public void threadStop() {
        is_stopped = true;
    }

    @Override
    public void run() {
        while (!is_stopped || queue[start_read] != null) {
            StringBuilder Messages = new StringBuilder();

            //キューを読む（文字）
            while (queue[start_read] != null && queue[start_read] instanceof String) {
                Messages.append(queue[start_read]).append("\n");
                queue[start_read] = null;

                start_read++;
                if (start_read >= QUEUE_BUF) {
                    start_read = 0;
                }
            }

            if(!Messages.toString().equals("")) {
                //制限が2000文字なので1900文字で区切る
                Matcher m = Pattern.compile("[\\s\\S]{1,1900}").matcher(Messages.toString());
                while (m.find()) {
                    channel.sendMessage(m.group());
                }
            }

            //キューを読む（埋め込み）
            while (queue[start_read] != null && queue[start_read] instanceof MessageEmbed) {
                channel.sendMessage((MessageEmbed) queue[start_read]);
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

package work.novablog.mcplugin.discordconnect.util;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordSender extends Thread{
    private final int QUEUE_BUF = 50;

    private TextChannel channel;
    private int startWrite = 0;
    private int startRead = 0;
    private final Object[] queue = new Object[QUEUE_BUF];

    private boolean isStopped = false;

    public DiscordSender(TextChannel channel) {
        this.channel = channel;
    }

    /**
     * キューに送信するメッセージを追加する
     * @param text 送信するメッセージ
     */
    public void addQueue(String text) {
        if(text.equals("")) return;

        queue[startWrite] = text;

        startWrite++;
        if (startWrite >= QUEUE_BUF) {
            startWrite = 0;
        }
    }

    /**
     * キューに送信する埋め込みメッセージを追加する
     * @param embed 送信する埋め込みメッセージ
     */
    public void addQueue(MessageEmbed embed) {
        queue[startWrite] = embed;

        startWrite++;
        if (startWrite >= QUEUE_BUF) {
            startWrite = 0;
        }
    }

    /**
     * スレッドを停止する
     */
    public void threadStop() {
        isStopped = true;
    }

    @Override
    public void run() {
        while (!isStopped || queue[startRead] != null) {
            StringBuilder Messages = new StringBuilder();

            //キューを読む（文字）
            while (queue[startRead] != null && queue[startRead] instanceof String) {
                Messages.append(queue[startRead]).append("\n");
                queue[startRead] = null;

                startRead++;
                if (startRead >= QUEUE_BUF) {
                    startRead = 0;
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
            while (queue[startRead] != null && queue[startRead] instanceof MessageEmbed) {
                channel.sendMessage((MessageEmbed) queue[startRead]);
                queue[startRead] = null;

                startRead++;
                if (startRead >= QUEUE_BUF) {
                    startRead = 0;
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

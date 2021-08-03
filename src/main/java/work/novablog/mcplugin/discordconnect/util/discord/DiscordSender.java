package work.novablog.mcplugin.discordconnect.util.discord;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordSender extends Thread{
    private final TextChannel channel;
    private final Queue<Object> queue = new ArrayDeque<>();
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
        queue.add(text);
    }

    /**
     * キューに送信する埋め込みメッセージを追加する
     * @param embed 送信する埋め込みメッセージ
     */
    public void addQueue(MessageEmbed embed) {
        queue.add(embed);
    }

    /**
     * スレッドを停止する
     */
    public void threadStop() {
        isStopped = true;
    }

    @Override
    public void run() {
        while (!isStopped || !queue.isEmpty()) {
            StringBuilder messages = new StringBuilder();

            //キューを読む（文字）
            while (!queue.isEmpty() && queue.peek() instanceof String) {
                messages.append(queue.poll()).append("\n");
            }

            if(!messages.toString().equals("")) {
                //制限が2000文字なので1900文字で区切る
                Matcher m = Pattern.compile("[\\s\\S]{1,1900}").matcher(messages.toString());
                while (m.find()) {
                    channel.sendMessage(m.group()).complete();
                }
            }

            //キューを読む（埋め込み）
            while (!queue.isEmpty() && queue.peek() instanceof MessageEmbed) {
                channel.sendMessage((MessageEmbed) queue.poll()).complete();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

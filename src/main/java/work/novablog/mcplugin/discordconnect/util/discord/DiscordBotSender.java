package work.novablog.mcplugin.discordconnect.util.discord;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordBotSender extends Thread{
    private final TextChannel channel;
    private final LinkedBlockingDeque<Object> queue;
    private boolean isInterrupted;

    /**
     * Discordの特定のテキストチャンネルへメッセージを送信するためのインスタンスを生成します
     * <p>
     * キュー方式でメッセージをためて、一気に送信することでレート制限に引っかかりにくくしています<br>
     * スレッドを停止したい場合はthread.interrupt()で割り込みを行ってください
     * </p>
     * @param channel メッセージを送信するチャンネル
     */
    public DiscordBotSender(@NotNull TextChannel channel) {
        this.channel = channel;
        queue = new LinkedBlockingDeque<>();
        isInterrupted = false;
    }

    /**
     * キューに送信するプレーンメッセージを追加します
     * @param text 送信するメッセージ
     */
    public void addQueue(@NotNull String text) {
        if(text.equals("")) return;
        queue.add(text);
    }

    /**
     * キューに送信する埋め込みメッセージを追加します
     * @param embed 送信する埋め込みメッセージ
     */
    public void addQueue(@NotNull MessageEmbed embed) {
        queue.add(embed);
    }

    /**
     * チャンネルIDを取得します
     * @return チャンネルID
     */
    public long getChannelID() {
        return channel.getIdLong();
    }

    @Override
    public void run() {
        while (!isInterrupted || !queue.isEmpty()) {
            try {
                // 要素が入るまで待機
                queue.addFirst(queue.take());
            } catch(InterruptedException e) {
                isInterrupted = true;
            }

            StringBuilder messages = new StringBuilder();

            //キューを読む（文字）
            while (queue.peek() instanceof String) {
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
            while (queue.peek() instanceof MessageEmbed) {
                channel.sendMessageEmbeds((MessageEmbed) queue.poll()).complete();
            }
        }
    }
}

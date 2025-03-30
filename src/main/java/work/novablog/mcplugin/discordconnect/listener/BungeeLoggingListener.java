package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.util.BotManager;

import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * BungeeCordのコンソールログをDiscordに送信するためのリスナー
 */
public class BungeeLoggingListener extends Handler {
    private final BotManager botManager;

    public BungeeLoggingListener(@NotNull BotManager botManager, @NotNull String logFormat) {
        this.botManager = botManager;
        setLevel(Level.INFO);
        setFormatter(new LogFormatter(logFormat));
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) return;
        botManager.sendMessageToChannel(BotManager.ChannelType.CONSOLE, getFormatter().format(record));
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    private static class LogFormatter extends Formatter {
        private final SimpleDateFormat sdf;
        private final String format;

        public LogFormatter(@NotNull String format) {
            sdf = new SimpleDateFormat("HH:mm:ss");
            this.format = format;
        }

        @Override
        public String format(LogRecord record) {
            MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(formatMessage(record), '§');
            String convertedMessage = MarkdownConverter.toDiscordMessage(components);

            return format.replace("{time}", sdf.format(record.getMillis()))
                    .replace("{level}", record.getLevel().getLocalizedName())
                    .replace("{logger}", record.getLoggerName())
                    .replace("{message}", convertedMessage);
        }
    }
}

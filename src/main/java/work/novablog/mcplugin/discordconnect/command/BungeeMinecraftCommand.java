package work.novablog.mcplugin.discordconnect.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.io.IOException;

/**
 * BungeeCordコマンド
 */
public class BungeeMinecraftCommand extends BungeeCommandExecutor {
    private static final String NAME = DiscordConnect.getInstance().getDescription().getName();
    public static final String PERM = "discordconnect.command";
    private static final String ALIASES = "discon";

    private static final String RELOAD_PERM = "reload";

    /**
     * コンストラクタ
     */
    public BungeeMinecraftCommand() {
        super(NAME, PERM, ALIASES);
        addSubCommand(new BungeeSubCommandBuilder("help", this::helpCmd).setDefault(true));
        addSubCommand(new BungeeSubCommandBuilder("reload", RELOAD_PERM, this::reloadCmd));
    }

    public void helpCmd(CommandSender sender, String[] args) {
        sender.sendMessage(new TextComponent(Message.bungeeCommandHelpLine1.toString()));
        sender.sendMessage(new TextComponent(Message.bungeeCommandHelpHelpcmd.toString()));
        sender.sendMessage(new TextComponent(Message.bungeeCommandHelpReloadcmd.toString()));
    }

    public void reloadCmd(CommandSender sender, String[] args) {
        try {
            DiscordConnect.getInstance().loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sender.sendMessage(new TextComponent(Message.configReloaded.toString()));
    }
}

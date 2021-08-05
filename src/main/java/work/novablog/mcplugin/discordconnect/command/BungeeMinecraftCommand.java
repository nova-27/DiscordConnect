package work.novablog.mcplugin.discordconnect.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;

public class BungeeMinecraftCommand extends BungeeCommandExecutor {
    private static final String NAME = DiscordConnect.getInstance().getDescription().getName();
    public static final String PERM = "discordconnect.command";
    private static final String ALIASES = "discon";

    private static final String RELOAD_PERM = "reload";
    private static final String DEBUG_PERM = "debug";

    public BungeeMinecraftCommand() {
        super(NAME, PERM, ALIASES);
        addSubCommand(new BungeeSubCommand("help", null, this::helpCmd).setDefault(true));
        addSubCommand(new BungeeSubCommand("reload", RELOAD_PERM, this::reloadCmd));
        addSubCommand(new BungeeSubCommand("debug", DEBUG_PERM, this::debugCmd));
    }

    public void helpCmd(CommandSender sender, String[] args) {
        sender.sendMessage(new TextComponent(ConfigManager.Message.bungeeCommandHelpLine1.toString()));
        sender.sendMessage(new TextComponent(ConfigManager.Message.bungeeCommandHelpHelpcmd.toString()));
        sender.sendMessage(new TextComponent(ConfigManager.Message.bungeeCommandHelpReloadcmd.toString()));
    }

    public void reloadCmd(CommandSender sender, String[] args) {
        DiscordConnect.getInstance().init();
        sender.sendMessage(new TextComponent(ConfigManager.Message.configReloaded.toString()));
    }

    public void debugCmd(CommandSender sender, String[] args) {
        sender.sendMessage(new TextComponent("*Bot"));
        sender.sendMessage(new TextComponent("isActive: " + DiscordConnect.getInstance().canBotBeUsed()));
    }
}

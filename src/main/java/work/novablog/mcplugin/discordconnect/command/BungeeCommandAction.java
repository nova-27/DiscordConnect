package work.novablog.mcplugin.discordconnect.command;

import net.md_5.bungee.api.CommandSender;

public interface BungeeCommandAction {
    /**
     * 実行する処理
     * @param sender コマンドの送信者
     * @param args 引数
     */
    void execute(CommandSender sender, String[] args);
}

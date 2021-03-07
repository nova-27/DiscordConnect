package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import work.novablog.mcplugin.discordconnect.DiscordConnect;

public class BungeeListener implements Listener {
    private final String toDiscordFormat;

    public BungeeListener(String toDiscordFormat) {
        this.toDiscordFormat = toDiscordFormat;
    }

    /**
     * チャットが送信されたら実行
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(ChatEvent event) {
        //コマンドなら
        if(event.isCommand() || event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer)) return;

        //N8ChatCasterAPI chatCasterApi = DiscordConnect.getInstance().getChatCasterApi();
        //if (chatCasterApi == null || !chatCasterApi.isEnabledChatCaster()) {
            // 連携プラグインが無効の場合
            ProxiedPlayer sender = (ProxiedPlayer)event.getSender();
            String senderServer = sender.getServer().getInfo().getName();
            String message = event.getMessage();

            MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
            String convertedMessage = MarkdownConverter.toDiscordMessage(components);
            DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel(
                    toDiscordFormat.replace("{server}", senderServer)
                            .replace("{sender}", sender.getName())
                            .replace("{message}", convertedMessage)
            );
        //}
    }

    /**
     * ログインされたら
     * @param e ログイン情報
     */
    /**@EventHandler
    public void onLogin(LoginEvent e) {
        String name = e.getConnection().getName();
        DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel("", "");
        DiscordConnect.getInstance().getBotManager().embed(Color.BLUE, Messages.joined.toString().replace("{name}", name), null);

        updatePlayerCount();
    }**/

    /**
     * 切断されたら
     * @param e 切断情報
     */
    /**@EventHandler
    public void onLogout(PlayerDisconnectEvent e) {
        String name = e.getPlayer().getName();
        DiscordConnect.getInstance().embed(Color.BLUE, Messages.left.toString().replace("{name}", name), null);

        updatePlayerCount();
    }**/

    /**
     * サーバー間を移動したら
     * @param e プレイヤー情報
     */
    /**@EventHandler
    public void onSwitch(ServerSwitchEvent e) {
        String name = e.getPlayer().getName();
        DiscordConnect.getInstance().embed(Color.CYAN, Messages.serverSwitched.toString().replace("{name}", name).replace("{server}", e.getPlayer().getServer().getInfo().getName()), null);
    }**/

    /**
     * プレイヤー数情報を更新
     */
    /**private void updatePlayerCount() {
        DiscordConnect.getInstance().getProxy().getScheduler().schedule(DiscordConnect.getInstance(), () -> {
            int maxPlayerInt = DiscordConnect.getInstance().getProxy().getConfig().getPlayerLimit();
            String maxPlayer = maxPlayerInt != -1 ? String.valueOf(maxPlayerInt) : "∞";

            DiscordConnect.getInstance().setGame(com.github.nova_27.mcplugin.discordconnect.ConfigData.playingGame
                    .replace("{players}", String.valueOf(DiscordConnect.getInstance().getProxy().getPlayers().size()))
                    .replace("{max}", maxPlayer)
            );
        },1L, TimeUnit.SECONDS);
    }**/
}

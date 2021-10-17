package work.novablog.mcplugin.discordconnect.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordBotSender;

import java.awt.*;
import java.util.ArrayList;

public class DiscordStandardCommand implements DiscordCommandListener {
    private final ArrayList<MessageEmbed.Field> help_cmd_fields;

    /**
     * 標準的なDiscord向けコマンドを追加します
     */
    public DiscordStandardCommand() {
        help_cmd_fields = new ArrayList<>();
    }

    /**
     * helpコマンドに新たなFieldを追加します
     * <p>
     *     独自コマンドを追加した際にこのメソッドを利用することで、
     *     コマンド一覧表に独自コマンドを追加することができます
     * </p>
     * @param field コマンドの説明
     */
    public void addHelpCmdField(MessageEmbed.Field field) {
        help_cmd_fields.add(field);
    }

    @DiscordCommandAnnotation("help")
    public void helpCmd(Member member, DiscordBotSender channel, String[] args) {
        String nickname = member.getNickname() == null ?
                member.getUser().getName() : member.getNickname();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(nickname, null, member.getUser().getAvatarUrl());
        eb.setTitle(ConfigManager.Message.discordCommandHelp.toString());
        eb.setColor(Color.orange);
        help_cmd_fields.forEach(eb::addField);

        eb.addField(
                "players",
                ConfigManager.Message.discordCommandPlayersListDescription.toString(),
                false
        );

        eb.addField(
                "reload",
                ConfigManager.Message.discordCommandReloadDescription.toString(),
                false
        );

        channel.addQueue(eb.build());
    }

    @DiscordCommandAnnotation("players")
    public void playersCmd(Member member, DiscordBotSender channel, String[] args) {
        ProxyServer proxy = DiscordConnect.getInstance().getProxy();

        String nickname = member.getNickname() == null ?
                member.getUser().getName() : member.getNickname();

        String maxPlayers = proxy.getConfig().getPlayerLimit() != -1 ?
                String.valueOf(proxy.getConfig().getPlayerLimit()) : "∞";

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(nickname, null, member.getUser().getAvatarUrl());
        eb.setTitle(ConfigManager.Message.discordCommandPlayerList.toString());
        eb.setColor(Color.orange);

        eb.setDescription(ConfigManager.Message.discordCommandPlayerCount.toString()
                .replace("{count}", String.valueOf(proxy.getOnlineCount()))
                .replace("{max}", maxPlayers)
        );

        proxy.getServers().forEach((name, server) -> {
            StringBuilder player_list_builder = new StringBuilder();
            for (ProxiedPlayer player : server.getPlayers()) {
                player_list_builder.append(player.getName()).append("\n");
            }
            String player_list = player_list_builder.toString().isEmpty() ?
                    ConfigManager.Message.discordCommandNoPlayersFound.toString() : player_list_builder.toString();
            eb.addField(name, player_list, true);
        });

        channel.addQueue(eb.build());
    }

    @DiscordCommandAnnotation(value = "reload", onlyAdmin = true)
    public void reloadCmd(Member member, DiscordBotSender channel, String[] args) {
        String nickname = member.getNickname() == null ?
                member.getUser().getName() : member.getNickname();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(nickname, null, member.getUser().getAvatarUrl());
        eb.setTitle(ConfigManager.Message.discordCommandReload.toString());
        eb.setColor(Color.orange);

        eb.setDescription(ConfigManager.Message.discordCommandReloading.toString());
        channel.addQueue(eb.build());
        DiscordConnect.getInstance().init();
    }
}

package work.novablog.mcplugin.discordconnect.command;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class DiscordStandardCommands implements DiscordCommandListener {
    @DiscordCommandAnnotation(value = "test", description = "テストコマンド")
    public void testCmd(SlashCommandEvent event) {
        event.reply("はろー！").queue();
    }
}

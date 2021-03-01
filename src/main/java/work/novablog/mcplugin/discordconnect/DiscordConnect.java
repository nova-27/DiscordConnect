package work.novablog.mcplugin.discordconnect;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.md_5.bungee.api.plugin.Plugin;
import net.dv8tion.jda.api.JDA;
import work.novablog.mcplugin.discordconnect.listener.DiscordListener;

import javax.security.auth.login.LoginException;

public final class DiscordConnect extends Plugin {
    String TOKEN = "";
    JDA bot;

    @Override
    public void onEnable() {
        try {
            bot = JDABuilder.createDefault(TOKEN)
                    .setActivity(Activity.playing("name"))
                    .addEventListeners(new DiscordListener())
                    .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        bot.shutdown();
    }
}

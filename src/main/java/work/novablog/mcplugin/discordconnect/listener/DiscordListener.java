package work.novablog.mcplugin.discordconnect.listener;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

public class DiscordListener implements EventListener {
    @Override
    public void onEvent(@NotNull GenericEvent event)
    {
        if(event instanceof ReadyEvent)
            System.out.println("API is ready!");
    }
}

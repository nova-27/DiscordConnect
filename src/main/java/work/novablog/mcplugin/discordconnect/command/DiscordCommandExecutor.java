package work.novablog.mcplugin.discordconnect.command;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class DiscordCommandExecutor extends ListenerAdapter {
    CommandListUpdateAction commands;
    ArrayList<SubcommandData> subCommands;
    ArrayList<DiscordSubCommandSetting> subCommandSettings;

    public DiscordCommandExecutor(CommandListUpdateAction commands) {
        this.commands = commands;
        this.subCommands = new ArrayList<>();
        this.subCommandSettings = new ArrayList<>();
    }

    /**
     * Discordコマンドを登録します
     * @param listener discordコマンドを処理するリスナー
     */
    public void registerCommand(@NotNull DiscordCommandListener listener) {
        for (Method m : listener.getClass().getMethods()) {
            DiscordCommandAnnotation annotation = m.getAnnotation(DiscordCommandAnnotation.class);
            if (annotation == null) continue;

            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1) {
                //引数の数が一致しない
                throw new IllegalArgumentException("handlers must receive one params.");
            }
            if (!params[0].equals(SlashCommandEvent.class)) {
                //引数の型が不正
                throw new IllegalArgumentException("the param type is incorrect.");
            }

            DiscordSubCommandSetting subCommandSetting =
                    new DiscordSubCommandSetting(annotation.value(), m, listener);
            subCommandSettings.add(subCommandSetting);
            SubcommandData subCommand = new SubcommandData(annotation.value(), annotation.description());
            subCommands.add(subCommand);
            commands.addCommands(new CommandData("discon", "DiscordConnect").addSubcommands(subCommands))
                    .queue();
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        DiscordSubCommandSetting subCommand = subCommandSettings.stream()
                .filter(setting -> setting.alias.equals(event.getSubcommandName())).findFirst().get();
        subCommand.execute(event);
    }

    public static class DiscordSubCommandSetting {
        private final String alias;
        private final Method action;
        private final Object instance;

        /**
         * サブコマンドの設定等を保持するインスタンスを生成します
         * @param alias コマンドのエイリアス
         * @param action 実行する処理
         * @param instance actionメソッドを含むクラスのインスタンス
         *                 action.invoke メソッド呼び出し時に利用されます
         */
        public DiscordSubCommandSetting(@NotNull String alias, @NotNull Method action, @NotNull Object instance) {
            this.alias = alias;
            this.action = action;
            this.instance = instance;
        }

        /**
         * アクションを呼び出します
         * @param event 実行した人
         */
        public void execute(SlashCommandEvent event) {
            try {
                action.invoke(instance, event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}

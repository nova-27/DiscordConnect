package work.novablog.mcplugin.discordconnect.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class DiscordCommandExecutor extends ListenerAdapter {
    private final String globalCmdAlias;
    private final String adminRole;
    private final JDA bot;
    private final ArrayList<SubcommandData> subCommands;
    private final ArrayList<DiscordSubCommandSetting> subCommandSettings;

    public DiscordCommandExecutor(String globalCmdAlias, String adminRole, JDA bot) {
        this.globalCmdAlias = globalCmdAlias;
        this.adminRole = adminRole;
        this.bot = bot;
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

            if(annotation.receivePolicy() != DiscordCommandReceivePolicy.onlyFromGuild && annotation.onlyAdmin()) {
                // 受信元規定が"Guild以外" かつ 管理者ロールが必要 の時
                // Guild以外ではロールを確認できない
                throw new IllegalArgumentException("onlyAdmin cannot be true when policy is not onlyFromGuild.");
            }

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
                    new DiscordSubCommandSetting(annotation.value(), annotation.onlyAdmin(), annotation.receivePolicy(), m, listener);
            subCommandSettings.add(subCommandSetting);
            SubcommandData subCommand = new SubcommandData(annotation.value(), annotation.description());
            subCommands.add(subCommand);
            bot.upsertCommand(new CommandData(globalCmdAlias, "DiscordConnect").addSubcommands(subCommands)).queue();
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        Optional<DiscordSubCommandSetting> subCommandOptional = subCommandSettings.stream()
                .filter(setting -> setting.alias.equals(event.getSubcommandName())).findFirst();
        assert subCommandOptional.isPresent();
        DiscordSubCommandSetting subCommand = subCommandOptional.get();

        String nickname = (event.getMember() == null || event.getMember().getNickname() == null) ?
                event.getUser().getName() : event.getMember().getNickname();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(nickname, null, event.getUser().getAvatarUrl());
        eb.setTitle(Message.command.toString());
        eb.setColor(Color.RED);

        if (event.isFromGuild()) {
            if(subCommand.receivePolicy == DiscordCommandReceivePolicy.onlyFromDM) {
                eb.setDescription(Message.discordOnlyFromDM.toString());
                event.replyEmbeds(eb.build()).queue();
                return;
            }

            // 権限の確認
            boolean isAdmin = Objects.requireNonNull(event.getMember()).getRoles().stream().anyMatch(role -> role.getName().equals(adminRole));
            if (subCommand.onlyAdmin && !isAdmin) {
                eb.setDescription(Message.discordCommandDenied.toString());
                event.replyEmbeds(eb.build()).queue();
                return;
            }
        } else {
            if(subCommand.receivePolicy == DiscordCommandReceivePolicy.onlyFromGuild) {
                eb.setDescription(Message.discordOnlyFromGuild.toString());
                event.replyEmbeds(eb.build()).queue();
                return;
            }
        }

        subCommandOptional.get().execute(event);
    }

    public static class DiscordSubCommandSetting {
        private final String alias;
        private final boolean onlyAdmin;
        private final DiscordCommandReceivePolicy receivePolicy;
        private final Method action;
        private final Object instance;

        /**
         * サブコマンドの設定等を保持するインスタンスを生成します
         * @param alias コマンドのエイリアス
         * @param onlyAdmin trueの場合コマンドを実行できるのは管理者だけ
         * @param receivePolicy コマンド受信元の規定
         * @param action 実行する処理
         * @param instance actionメソッドを含むクラスのインスタンス
         *                 action.invoke メソッド呼び出し時に利用されます
         */
        public DiscordSubCommandSetting(
                @NotNull String alias,
                boolean onlyAdmin,
                DiscordCommandReceivePolicy receivePolicy,
                @NotNull Method action,
                @NotNull Object instance
        ) {
            this.alias = alias;
            this.onlyAdmin = onlyAdmin;
            this.receivePolicy = receivePolicy;
            this.action = action;
            this.instance = instance;
        }

        /**
         * アクションを呼び出します
         * @param event SlashCommandEvent
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

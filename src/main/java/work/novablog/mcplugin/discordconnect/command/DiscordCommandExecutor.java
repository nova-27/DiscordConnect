package work.novablog.mcplugin.discordconnect.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordBotSender;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class DiscordCommandExecutor {
    private String adminRole;
    private final ArrayList<DiscordCommandSettings> commands;

    /**
     * Discordコマンドの解析や処理の呼び出しを行うインスタンスを生成します
     */
    public DiscordCommandExecutor() {
        commands = new ArrayList<>();
    }

    /**
     * 管理者のロール名を設定します
     * <p>
     *     {@link DiscordCommandSettings#onlyAdmin onlyAdmin}が{@code true}のコマンドは
     *     管理者のロールがついたメンバーだけが実行できます。
     * </p>
     * @param adminRole 管理者のロール名
     */
    public void setAdminRole(@NotNull String adminRole) {
        this.adminRole = adminRole;
    }

    /**
     * Discordコマンドを登録します
     * <p>
     *     {@code listener}のメソッドは次のルールに従ってください。
     *     <ul>
     *         <li>{@link DiscordCommandAnnotation}アノテーションをつける</li>
     *         <li>割当済みのエイリアスは{@link DiscordCommandAnnotation#value()}に指定しない</li>
     *         <li>第一引数は{@link Member}</li>
     *         <li>第二引数は{@code String[]}</li>
     *     </ul>
     * </p>
     * @param listener discordコマンドを処理するリスナー
     * @throws IllegalArgumentException {@code listener}のメソッドが上記ルールに従っていない場合
     */
    public void registerCommand(@NotNull DiscordCommandListener listener) throws IllegalArgumentException {
        for (Method m : listener.getClass().getMethods()) {
            DiscordCommandAnnotation discordCommandAnnotation = m.getAnnotation(DiscordCommandAnnotation.class);
            if(discordCommandAnnotation == null) continue;

            Class<?>[] params = m.getParameterTypes();
            if(params.length != 2) {
                //引数の数が一致しない
                throw new IllegalArgumentException("handlers must receive 2 params.");
            }
            if(!params[0].equals(Member.class) || !params[1].equals(String[].class)) {
                //引数の型が不正
                throw new IllegalArgumentException("the param type is incorrect.");
            }

            String alias = discordCommandAnnotation.value();
            if(commands.stream().anyMatch(command -> command.alias.equals(alias))) {
                //エイリアスが使用済み
                throw new IllegalArgumentException("the alias is already used.");
            }

            DiscordCommandSettings newCommand =
                    new DiscordCommandSettings(alias, m, listener, discordCommandAnnotation.onlyAdmin())
                            .requireArgs(discordCommandAnnotation.requireArgs());
            commands.add(newCommand);
        }
    }

    /**
     * コマンドの解析、呼び出しを行います
     * <p>
     *     通常{@link work.novablog.mcplugin.discordconnect.listener.DiscordListener}によって呼び出されます。
     * </p>
     * @param e イベント情報
     * @param alias コマンドのエイリアス
     * @param args コマンドの引数
     */
    public void parse(@NotNull MessageReceivedEvent e, @NotNull String alias, @NotNull String[] args) {
        BotManager botManager = DiscordConnect.getInstance().getBotManager();
        assert botManager != null;
        Member sender = Objects.requireNonNull(e.getMember());
        String nickname = sender.getNickname() == null ?
                sender.getUser().getName() : sender.getNickname();
        Optional<DiscordBotSender> channel_optional = DiscordConnect.getInstance().getBotManager().getChatChannelSenders().stream()
                .filter(element -> element.getChannelID() == e.getChannel().getIdLong())
                .findFirst();
        assert channel_optional.isPresent();
        DiscordBotSender channel = channel_optional.get();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(nickname, null, sender.getUser().getAvatarUrl());
        eb.setTitle(ConfigManager.Message.command.toString());
        eb.setColor(Color.RED);

        //サブコマンドを選択
        Optional<DiscordCommandSettings> targetSubCommand = commands
                .stream()
                .filter(subCommand -> subCommand.alias.equals(alias))
                .findFirst();
        if(!targetSubCommand.isPresent()) {
            //エイリアスが一致するサブコマンドがない場合エラー
            eb.setDescription(ConfigManager.Message.discordCommandNotFound.toString());
            channel.addQueue(eb.build());
            return;
        }

        //権限の確認
        boolean isAdmin = sender.getRoles().stream().anyMatch(role -> role.getName().equals(adminRole));
        if (targetSubCommand.get().onlyAdmin && !isAdmin) {
            eb.setDescription(ConfigManager.Message.discordCommandDenied.toString());
            channel.addQueue(eb.build());
            return;
        }

        //引数の確認
        if(args.length < targetSubCommand.get().requireArgs) {
            eb.setDescription(ConfigManager.Message.discordCommandSyntaxError.toString());
            channel.addQueue(eb.build());
            return;
        }

        targetSubCommand.get().execute(sender, args);
    }

    public static class DiscordCommandSettings {
        private final String alias;
        private final Method action;
        private final Object instance;
        private final boolean onlyAdmin;
        private int requireArgs;

        /**
         * コマンドの設定等を保持するインスタンスを生成します
         * @param alias コマンドのエイリアス
         * @param action 実行する処理
         * @param instance {@code action}メソッドを含むクラスのインスタンス
         *                 {@code action.invoke()}メソッド呼び出し時に利用されます。
         * @param onlyAdmin trueの場合コマンドを実行できるのは管理者だけ
         */
        public DiscordCommandSettings(@NotNull String alias, @NotNull Method action, @NotNull Object instance, boolean onlyAdmin) {
            this.alias = alias;
            this.action = action;
            this.instance = instance;
            this.onlyAdmin = onlyAdmin;
            requireArgs = 0;
        }

        /**
         * 必要な引数の数を設定します
         * <p>
         *     コマンド実行時、引数の数が足りていなかったらエラーメッセージが出ます。
         * </p>
         * @param cnt 引数の数
         */
        public DiscordCommandSettings requireArgs(int cnt) {
            this.requireArgs = cnt;
            return this;
        }

        /**
         * アクションを呼び出します
         * @param member 実行した人
         * @param args 引数
         */
        public void execute(@NotNull Member member, @NotNull String[] args) {
            try {
                action.invoke(instance, member, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}

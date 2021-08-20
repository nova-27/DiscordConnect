package work.novablog.mcplugin.discordconnect.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DiscordCommandAnnotation {
    //コマンドのエイリアス
    String value();
    //管理者ロールを持っている人のみ実行可能か
    boolean onlyAdmin() default false;
    //必要な引数の数
    int requireArgs() default 0;
}

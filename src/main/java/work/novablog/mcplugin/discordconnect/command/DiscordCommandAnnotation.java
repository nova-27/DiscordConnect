package work.novablog.mcplugin.discordconnect.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DiscordCommandAnnotation {
    String value();
    boolean onlyAdmin() default false;
    int requireArgs() default 0;
}

package myau.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Command {
    public final ArrayList<String> names;

    public Command(ArrayList<String> names) {
        this.names = names;
    }

    public abstract void runCommand(ArrayList<String> args);

    // 兼容旧 Command
    public List<String> tabComplete(List<String> args) {
        return Collections.emptyList();
    }

    // 新补全接口
    public List<String> tabComplete(CompletionContext context) {
        return tabComplete(context.getArgs());
    }
}
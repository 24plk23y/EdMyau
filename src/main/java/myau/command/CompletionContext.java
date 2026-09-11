package myau.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CompletionContext {
    private final String input;
    private final String commandName;
    private final List<String> args;
    private final int currentIndex;
    private final String currentArg;

    public CompletionContext(String input, String commandName, List<String> args) {
        this.input = input;
        this.commandName = commandName;
        this.args = Collections.unmodifiableList(args);
        this.currentIndex = Math.max(0, args.size() - 1);
        this.currentArg = args.isEmpty() ? "" : args.get(currentIndex);
    }

    public String getInput() {
        return input;
    }

    public String getCommandName() {
        return commandName;
    }

    public List<String> getArgs() {
        return args;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public String getCurrentArg() {
        return currentArg;
    }

    public String[] getArgsArray() {
        return args.toArray(new String[0]);
    }

    public static CompletionContext fromInput(String input) {
        String raw = input == null ? "" : input;
        if (raw.startsWith(".")) raw = raw.substring(1);

        String[] split = raw.split("\\s+", -1);
        if (split.length == 0) return new CompletionContext(input, "", Collections.emptyList());

        String command = split[0];
        List<String> args;

        if (split.length > 1) {
            args = Arrays.asList(Arrays.copyOfRange(split, 1, split.length));
        } else {
            args = Collections.emptyList();
        }

        return new CompletionContext(input, command, args);
    }
}
package myau.command;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.PacketEvent;
import myau.util.ChatUtil;
import net.minecraft.network.play.client.C01PacketChatMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CommandManager {
    public final ArrayList<Command> commands = new ArrayList<>();
    public String[] latestAutoComplete = new String[0];

    public boolean isCommandInput(String input) {
        return input != null && input.startsWith(".");
    }

    public Command getCommand(String name) {
        if (name == null) return null;
        for (Command command : commands) {
            for (String alias : command.names) {
                if (alias.equalsIgnoreCase(name)) return command;
            }
        }
        return null;
    }

    public boolean autoComplete(String input) {
        latestAutoComplete = getCompletions(input);
        return isCommandInput(input);
    }

    public String[] getCompletions(String input) {
        if (!isCommandInput(input)) return new String[0];

        String raw = input.substring(1);
        if (raw.isEmpty()) return getCommandCompletions("");

        String[] split = parseInput(raw);
        String commandName = split[0];

        if (split.length == 1 && !hasTrailingSpace(raw)) {
            return getCommandCompletions(commandName);
        }

        Command command = getCommand(commandName);
        if (command == null) return new String[0];

        List<String> args = new ArrayList<>();
        if (split.length > 1) {
            args.addAll(Arrays.asList(Arrays.copyOfRange(split, 1, split.length)));
        } else {
            args.add("");
        }

        if (hasTrailingSpace(raw)) args.add("");

        CompletionContext context = new CompletionContext(input, commandName, args);
        List<String> completions = command.tabComplete(context);

        return filterAndSort(completions, context.getCurrentArg());
    }

    private String[] getCommandCompletions(String current) {
        List<String> result = new ArrayList<>();

        for (Command command : commands) {
            for (String name : command.names) {
                if (matches(name, current)) result.add("." + name);
            }
        }

        return filterAndSort(result, "." + current);
    }

    private String[] parseInput(String raw) {
        return raw.trim().isEmpty() ? new String[]{""} : raw.trim().split("\\s+");
    }

    private boolean hasTrailingSpace(String raw) {
        return !raw.isEmpty() && Character.isWhitespace(raw.charAt(raw.length() - 1));
    }

    public static boolean matches(String candidate, String input) {
        return normalize(candidate).startsWith(normalize(input));
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return value.replace("-", "").toLowerCase(Locale.ROOT);
    }

    private String[] filterAndSort(List<String> completions, String current) {
        if (completions == null || completions.isEmpty()) return new String[0];

        List<String> result = new ArrayList<>();
        String normalizedCurrent = normalize(current);

        for (String completion : completions) {
            if (completion == null) continue;
            if (normalize(completion).startsWith(normalizedCurrent)) result.add(completion);
        }

        Collections.sort(result, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                String na = normalize(a);
                String nb = normalize(b);
                String nc = normalizedCurrent;

                boolean aExact = na.equals(nc);
                boolean bExact = nb.equals(nc);
                if (aExact != bExact) return aExact ? -1 : 1;

                int aLength = na.length();
                int bLength = nb.length();
                if (aLength != bLength) return Integer.compare(aLength, bLength);

                return a.compareToIgnoreCase(b);
            }
        });

        return result.toArray(new String[0]);
    }

    public void handleCommand(String string) {
        if (!isCommandInput(string)) return;

        String raw = string.substring(1).trim();
        if (raw.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sUnknown command&r", Myau.clientName).replace("&", "§"));
            return;
        }

        ArrayList<String> params = new ArrayList<>(Arrays.asList(raw.split("\\s+")));
        Command command = getCommand(params.get(0));

        if (command != null) {
            command.runCommand(params);
            return;
        }

        ChatUtil.sendFormatted(String.format("%sUnknown command (&o%s&r)&r", Myau.clientName, params.get(0)).replace("&", "§"));
    }

    public boolean isTypingCommand(String string) {
        return string != null && string.length() >= 2 && string.charAt(0) == '.' && Character.isLetterOrDigit(string.charAt(1));
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C01PacketChatMessage) {
            String msg = ((C01PacketChatMessage) event.getPacket()).getMessage();
            if (isTypingCommand(msg)) {
                event.setCancelled(true);
                handleCommand(msg);
            }
        }
    }
}
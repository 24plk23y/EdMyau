package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.command.CompletionContext;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleCommand extends Command {
    public ModuleCommand() {
        super(new ArrayList<>(Myau.moduleManager.modules.values().stream()
                .map(Module::getName)
                .collect(Collectors.toList())));
    }

    @Override
    public List<String> tabComplete(CompletionContext context) {
        List<String> result = new ArrayList<>();
        Module module = Myau.moduleManager.getModule(context.getCommandName());
        if (module == null) return result;

        ArrayList<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
        if (properties == null) return result;

        int index = context.getCurrentIndex();

        if (index == 0) {
            for (Property<?> property : properties) {
                if (property.isVisible()) result.add(property.getName());
            }
            return result;
        }

        Property<?> property = Myau.propertyManager.getProperty(module, context.getArgs().get(0));
        if (property == null || !property.isVisible()) return result;

        if (index == 1) {
            if (property instanceof ModeProperty) {
                ModeProperty mode = (ModeProperty) property;
                for (String value : getModes(mode)) result.add(value);
            } else if (property instanceof BooleanProperty) {
                result.add("true");
                result.add("false");
                result.add("on");
                result.add("off");
            }
        }

        return result;
    }

    private List<String> getModes(ModeProperty property) {
        List<String> result = new ArrayList<>();
        String prompt = property.getValuePrompt();

        if (prompt.isEmpty()) return result;

        for (String mode : prompt.split(", ")) result.add(mode);
        return result;
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        Module module = Myau.moduleManager.getModule(args.get(0));
        if (module == null) return;

        if (args.size() >= 2) {
            Property<?> property = Myau.propertyManager.getProperty(module, args.get(1));

            if (property == null) {
                ChatUtil.sendFormatted(String.format("%s%s has no property &o%s&r", Myau.clientName, module.getName(), args.get(1)));
                return;
            }

            if (args.size() < 3 && !(property instanceof BooleanProperty)) {
                ChatUtil.sendFormatted(String.format("%s%s: &o%s&r is set to %s&r (%s)&r",
                        Myau.clientName, module.getName(), property.getName(), property.formatValue(), property.getValuePrompt()));
                return;
            }

            String newValue = args.size() < 3 ? null : String.join(" ", args.subList(2, args.size()));

            try {
                if (property.parseString(newValue)) {
                    ChatUtil.sendFormatted(String.format("%s%s: &o%s&r has been set to %s&r",
                            Myau.clientName, module.getName(), property.getName(), property.formatValue()));
                    return;
                }
            } catch (Exception ignored) {
            }

            ChatUtil.sendFormatted(String.format("%sInvalid value for property &o%s&r (%s)&r",
                    Myau.clientName, property.getName(), property.getValuePrompt()));
            return;
        }

        List<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
        if (properties != null) {
            List<Property<?>> visible = properties.stream().filter(Property::isVisible).collect(Collectors.toList());
            if (!visible.isEmpty()) {
                ChatUtil.sendFormatted(String.format("%s%s:&r", Myau.clientName, module.formatModule()));
                for (Property<?> property : visible) {
                    ChatUtil.sendFormatted(String.format("&7»&r %s: %s&r", property.getName(), property.formatValue()));
                }
                return;
            }
        }

        ChatUtil.sendFormatted(String.format("%s%s has no properties&r", Myau.clientName, module.formatModule()));
    }
}
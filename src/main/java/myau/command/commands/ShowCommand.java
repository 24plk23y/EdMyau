package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.module.Module;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ShowCommand extends Command {

    public ShowCommand() {
        super(new ArrayList<>(Arrays.asList(
                "show",
                "s",
                "unhide"
        )));
    }

    @Override
    public List<String> tabComplete(List<String> args) {
        List<String> result = new ArrayList<>();

        /*
         * .show <TAB>
         *
         * 补全：
         * *
         * 所有模块名称
         */
        if (args.size() == 1) {

            result.add("*");

            for (Module module :
                    Myau.moduleManager.modules.values()) {

                if (module == null) {
                    continue;
                }

                String name = module.getName();

                if (name != null && !name.isEmpty()) {
                    result.add(name);
                }
            }
        }

        return result;
    }

    @Override
    public void runCommand(ArrayList<String> args) {

        if (args.size() < 2) {

            ChatUtil.sendFormatted(
                    String.format(
                            "%sUsage: .%s <&omodule&r>&r",
                            Myau.clientName,
                            args.get(0).toLowerCase(Locale.ROOT)
                    )
            );

        } else if (!args.get(1).equals("*")) {

            Module module =
                    Myau.moduleManager.getModule(args.get(1));

            if (module == null) {

                ChatUtil.sendFormatted(
                        String.format(
                                "%sModule &o%s&r not found&r",
                                Myau.clientName,
                                args.get(1)
                        )
                );

            } else if (!module.isHidden()) {

                ChatUtil.sendFormatted(
                        String.format(
                                "%s&o%s&r is not hidden in HUD&r",
                                Myau.clientName,
                                module.getName()
                        )
                );

            } else {

                module.setHidden(false);

                ChatUtil.sendFormatted(
                        String.format(
                                "%s&o%s&r is no longer hidden in HUD&r",
                                Myau.clientName,
                                module.getName()
                        )
                );
            }

        } else {

            for (Module module :
                    Myau.moduleManager.modules.values()) {

                module.setHidden(false);
            }

            ChatUtil.sendFormatted(
                    String.format(
                            "%sAll modules are no longer hidden in HUD&r",
                            Myau.clientName
                    )
            );
        }
    }
}
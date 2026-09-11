package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TargetCommand extends Command {

    private static final Minecraft mc =
            Minecraft.getMinecraft();

    public TargetCommand() {
        super(new ArrayList<>(Arrays.asList(
                "enemy",
                "e",
                "target"
        )));
    }

    @Override
    public List<String> tabComplete(List<String> args) {

        List<String> result = new ArrayList<>();

        /*
         * ============================================================
         * .target <TAB>
         * ============================================================
         */
        if (args.size() == 1) {

            result.add("add");
            result.add("remove");
            result.add("list");
            result.add("clear");

            return result;
        }

        String subCommand =
                args.get(0).toLowerCase(Locale.ROOT);

        /*
         * ============================================================
         * .target add <TAB>
         *
         * 当前服务器玩家
         *
         * ============================================================
         */
        if (subCommand.equals("add")) {

            if (mc.getNetHandler() == null) {
                return result;
            }

            for (NetworkPlayerInfo playerInfo :
                    mc.getNetHandler().getPlayerInfoMap()) {

                if (playerInfo == null
                        || playerInfo.getGameProfile() == null) {
                    continue;
                }

                String name =
                        playerInfo.getGameProfile().getName();

                if (name == null || name.isEmpty()) {
                    continue;
                }

                /*
                 * 已经是 enemy 的玩家不需要再次添加。
                 */
                if (Myau.targetManager.getPlayers().contains(name)) {
                    continue;
                }

                result.add(name);
            }

            return result;
        }

        /*
         * ============================================================
         * .target remove <TAB>
         *
         * 已经存在于 enemy 列表中的玩家
         *
         * ============================================================
         */
        if (subCommand.equals("remove")) {

            result.addAll(
                    Myau.targetManager.getPlayers()
            );

            return result;
        }

        /*
         * list / clear 没有参数。
         */
        return result;
    }

    @Override
    public void runCommand(ArrayList<String> args) {

        if (args.size() >= 2) {

            String subCommand =
                    args.get(1).toLowerCase(Locale.ROOT);

            switch (subCommand) {

                /*
                 * ====================================================
                 * ADD
                 * ====================================================
                 */
                case "add":

                    if (args.size() < 3) {

                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sUsage: .%s add <&oname&r>&r",
                                        Myau.clientName,
                                        args.get(0)
                                                .toLowerCase(Locale.ROOT)
                                )
                        );

                        return;
                    }

                    String added =
                            Myau.targetManager.add(
                                    args.get(2)
                            );

                    if (added == null) {

                        ChatUtil.sendFormatted(
                                String.format(
                                        "%s&o%s&r is already in your enemy list&r",
                                        Myau.clientName,
                                        args.get(2)
                                )
                        );

                        return;
                    }

                    ChatUtil.sendFormatted(
                            String.format(
                                    "%sAdded &o%s&r to your enemy list&r",
                                    Myau.clientName,
                                    added
                            )
                    );

                    return;

                /*
                 * ====================================================
                 * REMOVE
                 * ====================================================
                 */
                case "remove":

                    if (args.size() < 3) {

                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sUsage: .%s remove <&oname&r>&r",
                                        Myau.clientName,
                                        args.get(0)
                                                .toLowerCase(Locale.ROOT)
                                )
                        );

                        return;
                    }

                    String removed =
                            Myau.targetManager.remove(
                                    args.get(2)
                            );

                    if (removed == null) {

                        ChatUtil.sendFormatted(
                                String.format(
                                        "%s&o%s&r is not in your enemy list&r",
                                        Myau.clientName,
                                        args.get(2)
                                )
                        );

                        return;
                    }

                    ChatUtil.sendFormatted(
                            String.format(
                                    "%sRemoved &o%s&r from your enemy list&r",
                                    Myau.clientName,
                                    removed
                            )
                    );

                    return;

                /*
                 * ====================================================
                 * LIST
                 * ====================================================
                 */
                case "list":

                    ArrayList<String> list =
                            Myau.targetManager.getPlayers();

                    if (list.isEmpty()) {

                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sNo enemies&r",
                                        Myau.clientName
                                )
                        );

                        return;
                    }

                    ChatUtil.sendFormatted(
                            String.format(
                                    "%sEnemies:&r",
                                    Myau.clientName
                            )
                    );

                    for (String player : list) {

                        ChatUtil.sendRaw(
                                String.format(
                                        ChatColors.formatColor(
                                                "   &o%s&r"
                                        ),
                                        player
                                )
                        );
                    }

                    return;

                /*
                 * ====================================================
                 * CLEAR
                 * ====================================================
                 */
                case "clear":

                    Myau.targetManager.clear();

                    ChatUtil.sendFormatted(
                            String.format(
                                    "%sCleared your enemy list&r",
                                    Myau.clientName
                            )
                    );

                    return;
            }
        }

        ChatUtil.sendFormatted(
                String.format(
                        "%sUsage: .%s <&oadd&r/&oremove&r/&olist&r/&oclear&r>&r",
                        Myau.clientName,
                        args.get(0)
                                .toLowerCase(Locale.ROOT)
                )
        );
    }
}
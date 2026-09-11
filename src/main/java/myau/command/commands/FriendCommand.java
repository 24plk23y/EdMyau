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

public class FriendCommand extends Command {

    public FriendCommand() {
        super(new ArrayList<>(Arrays.asList(
                "friend",
                "f"
        )));
    }

    @Override
    public List<String> tabComplete(List<String> args) {

        List<String> result = new ArrayList<>();

        /*
         * ============================================================
         * .friend <TAB>
         * ============================================================
         */
        if (args.size() == 1) {

            result.add("add");
            result.add("a");

            result.add("remove");
            result.add("r");

            result.add("list");
            result.add("l");

            result.add("clear");
            result.add("c");

            return result;
        }

        /*
         * ============================================================
         * .friend add <TAB>
         * .friend a <TAB>
         *
         * 补全当前服务器玩家。
         *
         * ============================================================
         */
        String subCommand =
                args.get(0).toLowerCase(Locale.ROOT);

        boolean isAdd =
                subCommand.equals("add")
                        || subCommand.equals("a");

        boolean isRemove =
                subCommand.equals("remove")
                        || subCommand.equals("r");

        if (!isAdd && !isRemove) {
            return result;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.getNetHandler() == null) {
            return result;
        }

        /*
         * 当前已经输入的玩家名字。
         *
         * 例如：
         *
         * .friend add Ste
         *
         * args:
         *
         * ["add", "Ste"]
         *
         * current = "Ste"
         *
         * CommandManager 会进一步负责 startsWith 过滤。
         */
        String current =
                args.get(args.size() - 1);

        /*
         * ============================================================
         * add
         *
         * 只显示还不是好友的人。
         *
         * ============================================================
         */
        if (isAdd) {

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
                 * 已经是好友的玩家不需要再次 add。
                 */
                if (Myau.friendManager.isFriend(name)) {
                    continue;
                }

                result.add(name);
            }
        }

        /*
         * ============================================================
         * remove
         *
         * 只显示已经是好友的人。
         *
         * ============================================================
         */
        if (isRemove) {

            /*
             * 这里不需要依赖当前服务器玩家列表。
             *
             * 因为好友列表中的玩家可能当前不在线。
             */
            for (String friend :
                    Myau.friendManager.getPlayers()) {

                if (friend == null || friend.isEmpty()) {
                    continue;
                }

                result.add(friend);
            }
        }

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
                case "a":
                case "add":

                    if (args.size() < 3) {

                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sUsage: .%s add <&oname&r> [&oname&r] ...&r",
                                        Myau.clientName,
                                        args.get(0).toLowerCase(Locale.ROOT)
                                )
                        );

                        return;
                    }

                    for (String name :
                            args.subList(2, args.size())) {

                        String added =
                                Myau.friendManager.add(name);

                        if (added == null) {

                            ChatUtil.sendFormatted(
                                    String.format(
                                            "%s&o%s&r is already in your friend list&r",
                                            Myau.clientName,
                                            name
                                    )
                            );

                        } else {

                            ChatUtil.sendFormatted(
                                    String.format(
                                            "%sAdded &o%s&r to your friend list&r",
                                            Myau.clientName,
                                            added
                                    )
                            );
                        }
                    }

                    return;

                /*
                 * ====================================================
                 * REMOVE
                 * ====================================================
                 */
                case "r":
                case "remove":

                    if (args.size() < 3) {

                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sUsage: .%s remove <&oname&r> [&oname&r] ...&r",
                                        Myau.clientName,
                                        args.get(0).toLowerCase(Locale.ROOT)
                                )
                        );

                        return;
                    }

                    for (String name :
                            args.subList(2, args.size())) {

                        String removed =
                                Myau.friendManager.remove(name);

                        if (removed == null) {

                            ChatUtil.sendFormatted(
                                    String.format(
                                            "%s&o%s&r is not in your friend list&r",
                                            Myau.clientName,
                                            name
                                    )
                            );

                        } else {

                            ChatUtil.sendFormatted(
                                    String.format(
                                            "%sRemoved &o%s&r from your friend list&r",
                                            Myau.clientName,
                                            removed
                                    )
                            );
                        }
                    }

                    return;

                /*
                 * ====================================================
                 * LIST
                 * ====================================================
                 */
                case "l":
                case "list":

                    ArrayList<String> list =
                            Myau.friendManager.getPlayers();

                    if (list.isEmpty()) {

                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sNo friends&r",
                                        Myau.clientName
                                )
                        );

                        return;
                    }

                    ChatUtil.sendFormatted(
                            String.format(
                                    "%sFriends:&r",
                                    Myau.clientName
                            )
                    );

                    for (String friend : list) {

                        ChatUtil.sendRaw(
                                String.format(
                                        ChatColors.formatColor(
                                                "   &o%s&r"
                                        ),
                                        friend
                                )
                        );
                    }

                    return;

                /*
                 * ====================================================
                 * CLEAR
                 * ====================================================
                 */
                case "c":
                case "clear":

                    Myau.friendManager.clear();

                    ChatUtil.sendFormatted(
                            String.format(
                                    "%sCleared your friend list&r",
                                    Myau.clientName
                            )
                    );

                    return;

                /*
                 * ====================================================
                 * 没有匹配到子命令
                 *
                 * 保留你原来的行为：
                 *
                 * .friend Steve
                 *
                 * 如果 Steve 是好友 -> remove
                 * 否则 -> add
                 *
                 * ====================================================
                 */
                default:

                    if (args.size() == 2) {

                        if (Myau.friendManager.isFriend(args.get(1))) {

                            runCommand(
                                    new ArrayList<>(
                                            Arrays.asList(
                                                    args.get(0),
                                                    "remove",
                                                    args.get(1)
                                            )
                                    )
                            );

                        } else {

                            runCommand(
                                    new ArrayList<>(
                                            Arrays.asList(
                                                    args.get(0),
                                                    "add",
                                                    args.get(1)
                                            )
                                    )
                            );
                        }

                        return;
                    }
            }
        }

        ChatUtil.sendFormatted(
                String.format(
                        "%sUsage: .%s <&oa(dd)&r/&or(emove)&r/&ol(ist)&r/&oc(lear)&r>&r",
                        Myau.clientName,
                        args.get(0).toLowerCase(Locale.ROOT)
                )
        );
    }
}
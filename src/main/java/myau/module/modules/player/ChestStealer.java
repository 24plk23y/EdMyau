package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.events.WindowClickEvent;
import myau.mixin.IAccessorItemSword;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ChatUtil;
import myau.util.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.List;

public class ChestStealer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private int clickDelay = 0;
    private int oDelay = 0;

    /*
     * Instant / Normal 完成后，
     * 在哪个 World Tick 关闭箱子。
     *
     * -1 = 没有等待关闭
     */
    private long closeAtTick = -1;

    private boolean inChest = false;
    private boolean warnedFull = false;

    /*
     * ============================================================
     * Properties
     * ============================================================
     */

    public final BooleanProperty instant =
            new BooleanProperty("instant", false);

    public final IntProperty minDelay =
            new IntProperty(
                    "min-delay",
                    1,
                    0,
                    20,
                    () -> !this.instant.getValue()
            );

    public final IntProperty maxDelay =
            new IntProperty(
                    "max-delay",
                    2,
                    0,
                    20,
                    () -> !this.instant.getValue()
            );

    public final IntProperty openDelay =
            new IntProperty(
                    "open-delay",
                    1,
                    0,
                    20
            );

    /*
     * 偷完以后等待多少 tick 再关闭。
     *
     * 0 = 当前 Tick 关闭
     * 1 = 下一 Tick 关闭
     * 2 = 两个 Tick 后关闭
     */
    public final IntProperty closeDelay =
            new IntProperty(
                    "close-delay",
                    1,
                    0,
                    20
            );

    public final BooleanProperty autoClose =
            new BooleanProperty(
                    "auto-close",
                    false
            );

    public final BooleanProperty nameCheck =
            new BooleanProperty(
                    "name-check",
                    true
            );

    public final BooleanProperty skipTrash =
            new BooleanProperty(
                    "skip-trash",
                    true
            );

    public final BooleanProperty moreArmor =
            new BooleanProperty(
                    "more-armor",
                    false
            );

    public final BooleanProperty moreSword =
            new BooleanProperty(
                    "more-sword",
                    false
            );

    private boolean isValidGameMode() {
        GameType gameType =
                mc.playerController.getCurrentGameType();

        return gameType == GameType.SURVIVAL
                || gameType == GameType.ADVENTURE;
    }

    private boolean isMoreArmor(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        if (!this.moreArmor.getValue()) {
            return false;
        }

        if (!(itemStack.getItem() instanceof ItemArmor)) {
            return false;
        }

        ItemArmor.ArmorMaterial armorMaterial =
                ((ItemArmor) itemStack.getItem())
                        .getArmorMaterial();

        if (armorMaterial ==
                ItemArmor.ArmorMaterial.DIAMOND) {

            return true;
        }

        return armorMaterial ==
                ItemArmor.ArmorMaterial.IRON
                && itemStack.isItemEnchanted();
    }

    private boolean isMoreSword(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        if (!this.moreSword.getValue()) {
            return false;
        }

        if (!(itemStack.getItem() instanceof ItemSword)) {
            return false;
        }

        Item.ToolMaterial swordMaterial =
                ((IAccessorItemSword) itemStack.getItem())
                        .getMaterial();

        if (swordMaterial ==
                Item.ToolMaterial.EMERALD) {

            return true;
        }

        if (EnchantmentHelper.getEnchantmentLevel(
                Enchantment.fireAspect.effectId,
                itemStack
        ) != 0) {
            return true;
        }

        return swordMaterial ==
                Item.ToolMaterial.IRON
                && itemStack.isItemEnchanted();
    }

    private boolean isInvManagerRequire(
            ItemStack itemStack
    ) {
        if (itemStack == null) {
            return false;
        }

        InvManager invManager =
                (InvManager) Myau.moduleManager.modules.get(
                        InvManager.class
                );

        if (ItemUtil.ItemType.Block.contains(itemStack)) {
            return !invManager.isEnabled()
                    || ItemUtil.findInventorySlot(
                    ItemUtil.ItemType.Block
            ) < invManager.blocks.getValue();
        }

        if (ItemUtil.ItemType.Projectile.contains(itemStack)) {
            return !invManager.isEnabled()
                    || ItemUtil.findInventorySlot(
                    ItemUtil.ItemType.Projectile
            ) < invManager.projectiles.getValue();
        }

        if (ItemUtil.ItemType.FishRod.contains(itemStack)) {
            return ItemUtil.findInventorySlot(
                    ItemUtil.ItemType.Projectile
            ) == 0;
        }

        if (ItemUtil.ItemType.Arrow.contains(itemStack)) {
            return !invManager.isEnabled()
                    || ItemUtil.findInventorySlot(
                    ItemUtil.ItemType.Arrow
            ) < invManager.arrow.getValue();
        }

        return false;
    }

    private boolean shouldTake(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        return !this.skipTrash.getValue()
                || !ItemUtil.isNotSpecialItem(itemStack)
                || isMoreArmor(itemStack)
                || isMoreSword(itemStack)
                || isInvManagerRequire(itemStack);
    }

    private void shiftClick(
            int windowId,
            int slotId
    ) {
        mc.playerController.windowClick(
                windowId,
                slotId,
                0,
                1,
                mc.thePlayer
        );
    }

    public ChestStealer() {
        super("ChestStealer", false);
    }

    /*
     * ============================================================
     * Schedule Close
     * ============================================================
     *
     * 统一处理 Instant / Normal 的 close-delay。
     */
    private void scheduleClose() {
        if (!this.autoClose.getValue()) {
            return;
        }

        int delay =
                this.closeDelay.getValue();

        if (delay <= 0) {
            mc.thePlayer.closeScreen();
            return;
        }

        this.closeAtTick =
                mc.theWorld.getTotalWorldTime()
                        + delay;
    }

    /*
     * ============================================================
     * Instant Mode
     * ============================================================
     */
    private void stealInstant(
            Container container,
            IInventory inventory
    ) {
        List<Integer> slotsToTake =
                new ArrayList<>();

        /*
         * ========================================================
         * 第一阶段：寻找箱子中最好的装备 / 工具
         * ========================================================
         */

        int bestSword = -1;
        double bestDamage = 0.0;

        int[] bestArmorSlots =
                new int[]{
                        -1, -1, -1, -1
                };

        double[] bestArmorProtection =
                new double[]{
                        0.0, 0.0, 0.0, 0.0
                };

        int bestPickaxeSlot = -1;
        float bestPickaxeEfficiency = 1.0F;

        int bestShovelSlot = -1;
        float bestShovelEfficiency = 1.0F;

        int bestAxeSlot = -1;
        float bestAxeEfficiency = 1.0F;

        int bestBow = -1;
        double bestBowDamage = 0.0;

        for (int i = 0;
             i < inventory.getSizeInventory();
             i++) {

            if (!container.getSlot(i).getHasStack()) {
                continue;
            }

            ItemStack stack =
                    container.getSlot(i).getStack();

            Item item =
                    stack.getItem();

            if (item instanceof ItemSword) {
                double damage =
                        ItemUtil.getAttackBonus(stack);

                if (bestSword == -1
                        || damage > bestDamage) {

                    bestSword = i;
                    bestDamage = damage;
                }

            } else if (item instanceof ItemArmor) {
                int armorType =
                        ((ItemArmor) item).armorType;

                double protectionLevel =
                        ItemUtil.getArmorProtection(stack);

                if (bestArmorSlots[armorType] == -1
                        || protectionLevel
                        > bestArmorProtection[armorType]) {

                    bestArmorSlots[armorType] = i;
                    bestArmorProtection[armorType] =
                            protectionLevel;
                }

            } else if (item instanceof ItemPickaxe) {
                float efficiency =
                        ItemUtil.getToolEfficiency(stack);

                if (bestPickaxeSlot == -1
                        || efficiency
                        > bestPickaxeEfficiency) {

                    bestPickaxeSlot = i;
                    bestPickaxeEfficiency =
                            efficiency;
                }

            } else if (item instanceof ItemSpade) {
                float efficiency =
                        ItemUtil.getToolEfficiency(stack);

                if (bestShovelSlot == -1
                        || efficiency
                        > bestShovelEfficiency) {

                    bestShovelSlot = i;
                    bestShovelEfficiency =
                            efficiency;
                }

            } else if (item instanceof ItemAxe) {
                float efficiency =
                        ItemUtil.getToolEfficiency(stack);

                if (bestAxeSlot == -1
                        || efficiency
                        > bestAxeEfficiency) {

                    bestAxeSlot = i;
                    bestAxeEfficiency =
                            efficiency;
                }

            } else if (item instanceof ItemBow) {
                double damage =
                        ItemUtil.getBowAttackBonus(stack);

                if (bestBow == -1
                        || damage > bestBowDamage) {

                    bestBow = i;
                    bestBowDamage = damage;
                }
            }
        }

        /*
         * ========================================================
         * 第二阶段：比较玩家已有装备
         * ========================================================
         */

        /*
         * Sword
         */
        if (bestSword != -1) {
            int swordInInventorySlot =
                    ItemUtil.findSwordInInventorySlot(
                            0,
                            true
                    );

            double inventoryDamage =
                    swordInInventorySlot != -1
                            ? ItemUtil.getAttackBonus(
                            mc.thePlayer.inventory
                                    .getStackInSlot(
                                            swordInInventorySlot
                                    )
                    )
                            : 0.0;

            if (bestDamage > inventoryDamage) {
                slotsToTake.add(bestSword);
            }
        }

        /*
         * Armor
         */
        for (int i = 0; i < 4; i++) {
            if (bestArmorSlots[i] == -1) {
                continue;
            }

            int slot =
                    ItemUtil.findArmorInventorySlot(
                            i,
                            true
                    );

            double protectionLevel =
                    slot != -1
                            ? ItemUtil.getArmorProtection(
                            mc.thePlayer.inventory
                                    .getStackInSlot(slot)
                    )
                            : 0.0;

            if (bestArmorProtection[i]
                    > protectionLevel) {

                slotsToTake.add(
                        bestArmorSlots[i]
                );
            }
        }

        /*
         * Pickaxe
         */
        int pickaxeSlot =
                ItemUtil.findInventorySlot(
                        "pickaxe",
                        0,
                        true
                );

        float pickaxeEfficiency =
                pickaxeSlot != -1
                        ? ItemUtil.getToolEfficiency(
                        mc.thePlayer.inventory
                                .getStackInSlot(
                                        pickaxeSlot
                                )
                )
                        : 1.0F;

        if (bestPickaxeSlot != -1
                && bestPickaxeEfficiency
                > pickaxeEfficiency) {

            slotsToTake.add(
                    bestPickaxeSlot
            );
        }

        /*
         * Shovel
         */
        int shovelSlot =
                ItemUtil.findInventorySlot(
                        "shovel",
                        0,
                        true
                );

        float shovelEfficiency =
                shovelSlot != -1
                        ? ItemUtil.getToolEfficiency(
                        mc.thePlayer.inventory
                                .getStackInSlot(
                                        shovelSlot
                                )
                )
                        : 1.0F;

        if (bestShovelSlot != -1
                && bestShovelEfficiency
                > shovelEfficiency) {

            slotsToTake.add(
                    bestShovelSlot
            );
        }

        /*
         * Axe
         */
        int axeSlot =
                ItemUtil.findInventorySlot(
                        "axe",
                        0,
                        true
                );

        float axeEfficiency =
                axeSlot != -1
                        ? ItemUtil.getToolEfficiency(
                        mc.thePlayer.inventory
                                .getStackInSlot(
                                        axeSlot
                                )
                )
                        : 1.0F;

        if (bestAxeSlot != -1
                && bestAxeEfficiency
                > axeEfficiency) {

            slotsToTake.add(
                    bestAxeSlot
            );
        }

        /*
         * Bow
         */
        int bowSlot =
                ItemUtil.findBowInventorySlot(
                        0,
                        true
                );

        double bowDamage =
                bowSlot != -1
                        ? ItemUtil.getBowAttackBonus(
                        mc.thePlayer.inventory
                                .getStackInSlot(
                                        bowSlot
                                )
                )
                        : 0.0;

        if (bestBow != -1
                && bestBowDamage > bowDamage) {

            slotsToTake.add(bestBow);
        }

        /*
         * ========================================================
         * 第三阶段：普通物品
         * ========================================================
         */

        for (int i = 0;
             i < inventory.getSizeInventory();
             i++) {

            if (!container.getSlot(i).getHasStack()) {
                continue;
            }

            ItemStack stack =
                    container.getSlot(i).getStack();

            if (shouldTake(stack)
                    && !slotsToTake.contains(i)) {

                slotsToTake.add(i);
            }
        }

        /*
         * ========================================================
         * 第四阶段：Instant 连续点击
         * ========================================================
         */

        for (int slot : slotsToTake) {
            if (slot < 0
                    || slot >= inventory.getSizeInventory()) {
                continue;
            }

            if (!container.getSlot(slot).getHasStack()) {
                continue;
            }

            this.shiftClick(
                    container.windowId,
                    slot
            );
        }

        /*
         * 偷完后按照 close-delay 关闭。
         */
        this.scheduleClose();
    }

    /*
     * ============================================================
     * Normal Mode
     * ============================================================
     *
     * 每次 Update 最多拿一个物品。
     *
     * 找到物品：
     *     shiftClick -> return
     *
     * 没有物品：
     *     scheduleClose()
     */
    private void stealNormal(
            Container container,
            IInventory inventory
    ) {
        /*
         * ========================================================
         * 最佳装备判断
         * ========================================================
         */

        if (this.skipTrash.getValue()) {
            int bestSword = -1;
            double bestDamage = 0.0;

            int[] bestArmorSlots =
                    new int[]{
                            -1, -1, -1, -1
                    };

            double[] bestArmorProtection =
                    new double[]{
                            0.0, 0.0, 0.0, 0.0
                    };

            int bestPickaxeSlot = -1;
            float bestPickaxeEfficiency = 1.0F;

            int bestShovelSlot = -1;
            float bestShovelEfficiency = 1.0F;

            int bestAxeSlot = -1;
            float bestAxeEfficiency = 1.0F;

            int bestBow = -1;
            double bestBowDamage = 0.0;

            for (int i = 0;
                 i < inventory.getSizeInventory();
                 i++) {

                if (!container.getSlot(i)
                        .getHasStack()) {
                    continue;
                }

                ItemStack stack =
                        container.getSlot(i)
                                .getStack();

                Item item =
                        stack.getItem();

                if (item instanceof ItemSword) {
                    double damage =
                            ItemUtil.getAttackBonus(stack);

                    if (bestSword == -1
                            || damage > bestDamage) {

                        bestSword = i;
                        bestDamage = damage;
                    }

                } else if (item instanceof ItemArmor) {
                    int armorType =
                            ((ItemArmor) item).armorType;

                    double protectionLevel =
                            ItemUtil.getArmorProtection(stack);

                    if (bestArmorSlots[armorType] == -1
                            || protectionLevel
                            > bestArmorProtection[armorType]) {

                        bestArmorSlots[armorType] = i;
                        bestArmorProtection[armorType] =
                                protectionLevel;
                    }

                } else if (item instanceof ItemPickaxe) {
                    float efficiency =
                            ItemUtil.getToolEfficiency(stack);

                    if (bestPickaxeSlot == -1
                            || efficiency
                            > bestPickaxeEfficiency) {

                        bestPickaxeSlot = i;
                        bestPickaxeEfficiency =
                                efficiency;
                    }

                } else if (item instanceof ItemSpade) {
                    float efficiency =
                            ItemUtil.getToolEfficiency(stack);

                    if (bestShovelSlot == -1
                            || efficiency
                            > bestShovelEfficiency) {

                        bestShovelSlot = i;
                        bestShovelEfficiency =
                                efficiency;
                    }

                } else if (item instanceof ItemAxe) {
                    float efficiency =
                            ItemUtil.getToolEfficiency(stack);

                    if (bestAxeSlot == -1
                            || efficiency
                            > bestAxeEfficiency) {

                        bestAxeSlot = i;
                        bestAxeEfficiency =
                                efficiency;
                    }

                } else if (item instanceof ItemBow) {
                    double damage =
                            ItemUtil.getBowAttackBonus(stack);

                    if (bestBow == -1
                            || damage > bestBowDamage) {

                        bestBow = i;
                        bestBowDamage = damage;
                    }
                }
            }

            /*
             * ====================================================
             * Sword
             * ====================================================
             */

            int swordInInventorySlot =
                    ItemUtil.findSwordInInventorySlot(
                            0,
                            true
                    );

            double damage =
                    swordInInventorySlot != -1
                            ? ItemUtil.getAttackBonus(
                            mc.thePlayer.inventory
                                    .getStackInSlot(
                                            swordInInventorySlot
                                    )
                    )
                            : 0.0;

            if (bestSword != -1
                    && bestDamage > damage) {

                this.shiftClick(
                        container.windowId,
                        bestSword
                );

                return;
            }

            /*
             * ====================================================
             * Armor
             * ====================================================
             */

            for (int i = 0; i < 4; i++) {
                int slot =
                        ItemUtil.findArmorInventorySlot(
                                i,
                                true
                        );

                double protectionLevel =
                        slot != -1
                                ? ItemUtil.getArmorProtection(
                                mc.thePlayer.inventory
                                        .getStackInSlot(slot)
                        )
                                : 0.0;

                if (bestArmorSlots[i] != -1
                        && bestArmorProtection[i]
                        > protectionLevel) {

                    this.shiftClick(
                            container.windowId,
                            bestArmorSlots[i]
                    );

                    return;
                }
            }

            /*
             * ====================================================
             * Pickaxe
             * ====================================================
             */

            int pickaxeSlot =
                    ItemUtil.findInventorySlot(
                            "pickaxe",
                            0,
                            true
                    );

            float pickaxeEfficiency =
                    pickaxeSlot != -1
                            ? ItemUtil.getToolEfficiency(
                            mc.thePlayer.inventory
                                    .getStackInSlot(
                                            pickaxeSlot
                                    )
                    )
                            : 1.0F;

            if (bestPickaxeSlot != -1
                    && bestPickaxeEfficiency
                    > pickaxeEfficiency) {

                this.shiftClick(
                        container.windowId,
                        bestPickaxeSlot
                );

                return;
            }

            /*
             * ====================================================
             * Shovel
             * ====================================================
             */

            int shovelSlot =
                    ItemUtil.findInventorySlot(
                            "shovel",
                            0,
                            true
                    );

            float shovelEfficiency =
                    shovelSlot != -1
                            ? ItemUtil.getToolEfficiency(
                            mc.thePlayer.inventory
                                    .getStackInSlot(
                                            shovelSlot
                                    )
                    )
                            : 1.0F;

            if (bestShovelSlot != -1
                    && bestShovelEfficiency
                    > shovelEfficiency) {

                this.shiftClick(
                        container.windowId,
                        bestShovelSlot
                );

                return;
            }

            /*
             * ====================================================
             * Axe
             * ====================================================
             */

            int axeSlot =
                    ItemUtil.findInventorySlot(
                            "axe",
                            0,
                            true
                    );

            float efficiency =
                    axeSlot != -1
                            ? ItemUtil.getToolEfficiency(
                            mc.thePlayer.inventory
                                    .getStackInSlot(
                                            axeSlot
                                    )
                    )
                            : 1.0F;

            if (bestAxeSlot != -1
                    && bestAxeEfficiency
                    > efficiency) {

                this.shiftClick(
                        container.windowId,
                        bestAxeSlot
                );

                return;
            }

            /*
             * ====================================================
             * Bow
             * ====================================================
             */

            int bowSlot =
                    ItemUtil.findBowInventorySlot(
                            0,
                            true
                    );

            double bowDamage =
                    bowSlot != -1
                            ? ItemUtil.getBowAttackBonus(
                            mc.thePlayer.inventory
                                    .getStackInSlot(
                                            bowSlot
                                    )
                    )
                            : 0.0;

            if (bestBow != -1
                    && bestBowDamage > bowDamage) {

                this.shiftClick(
                        container.windowId,
                        bestBow
                );

                return;
            }
        }

        /*
         * ========================================================
         * 普通物品
         * ========================================================
         */

        for (int i = 0;
             i < inventory.getSizeInventory();
             i++) {

            if (!container.getSlot(i)
                    .getHasStack()) {
                continue;
            }

            ItemStack stack =
                    container.getSlot(i)
                            .getStack();

            if (shouldTake(stack)) {
                this.shiftClick(
                        container.windowId,
                        i
                );

                return;
            }
        }

        /*
         * ========================================================
         * 没有可拿物品
         * ========================================================
         *
         * 这里与 Instant 一样使用 close-delay。
         */
        this.scheduleClose();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }

        /*
         * ============================================================
         * Pending Close
         * ============================================================
         */

        if (this.closeAtTick != -1) {
            /*
             * 如果已经不是箱子，
             * 清除等待关闭状态。
             */
            if (!(mc.currentScreen instanceof GuiChest)) {
                this.closeAtTick = -1;
            } else if (mc.theWorld != null
                    && mc.theWorld.getTotalWorldTime()
                    >= this.closeAtTick) {

                this.closeAtTick = -1;

                if (this.autoClose.getValue()) {
                    mc.thePlayer.closeScreen();
                }

                return;
            } else {
                /*
                 * 还没有到关闭 Tick。
                 */
                return;
            }
        }

        /*
         * ============================================================
         * Delay 更新
         * ============================================================
         */

        if (this.clickDelay > 0) {
            this.clickDelay--;
        }

        if (this.oDelay > 0) {
            this.oDelay--;
        }

        /*
         * ============================================================
         * 当前不是箱子
         * ============================================================
         */

        if (!(mc.currentScreen instanceof GuiChest)) {
            this.inChest = false;
            this.closeAtTick = -1;
            return;
        }

        Container container =
                ((GuiChest) mc.currentScreen)
                        .inventorySlots;

        if (!(container instanceof ContainerChest)) {
            this.inChest = false;
            this.closeAtTick = -1;
            return;
        }

        /*
         * ============================================================
         * 第一次进入箱子
         * ============================================================
         */

        if (!this.inChest) {
            this.inChest = true;
            this.warnedFull = false;
            this.closeAtTick = -1;

            this.oDelay =
                    this.openDelay.getValue() + 1;
        }

        /*
         * ============================================================
         * Instant Mode
         * ============================================================
         */

        if (this.instant.getValue()) {
            if (this.oDelay > 0) {
                return;
            }

            if (!this.isEnabled()
                    || !this.isValidGameMode()) {
                return;
            }

            IInventory inventory =
                    ((ContainerChest) container)
                            .getLowerChestInventory();

            /*
             * Name Check
             */
            if (this.nameCheck.getValue()) {
                String inventoryName =
                        inventory.getName();

                if (!inventoryName.equals(
                        I18n.format("container.chest"))
                        && !inventoryName.equals(
                        I18n.format(
                                "container.chestDouble"))) {

                    return;
                }
            }

            /*
             * 背包满检测
             */
            if (mc.thePlayer.inventory
                    .getFirstEmptyStack() == -1) {

                if (!this.warnedFull) {
                    ChatUtil.sendFormatted(
                            String.format(
                                    "%s%s: &cYour inventory is full!&r",
                                    Myau.clientName,
                                    this.getName()
                            )
                    );

                    this.warnedFull = true;
                }

                if (this.autoClose.getValue()) {
                    mc.thePlayer.closeScreen();
                }

                return;
            }

            /*
             * Instant：
             * 一次 Update 连续处理所有目标。
             */
            this.stealInstant(
                    container,
                    inventory
            );

            return;
        }

        /*
         * ============================================================
         * Normal Mode
         * ============================================================
         */

        if (this.oDelay <= 0
                && this.clickDelay <= 0) {

            if (!this.isEnabled()
                    || !this.isValidGameMode()) {
                return;
            }

            IInventory inventory =
                    ((ContainerChest) container)
                            .getLowerChestInventory();

            /*
             * Name Check
             */
            if (this.nameCheck.getValue()) {
                String inventoryName =
                        inventory.getName();

                if (!inventoryName.equals(
                        I18n.format("container.chest"))
                        && !inventoryName.equals(
                        I18n.format(
                                "container.chestDouble"))) {

                    return;
                }
            }

            /*
             * 背包满
             */
            if (mc.thePlayer.inventory
                    .getFirstEmptyStack() == -1) {

                if (!this.warnedFull) {
                    ChatUtil.sendFormatted(
                            String.format(
                                    "%s%s: &cYour inventory is full!&r",
                                    Myau.clientName,
                                    this.getName()
                            )
                    );

                    this.warnedFull = true;
                }

                if (this.autoClose.getValue()) {
                    mc.thePlayer.closeScreen();
                }

                return;
            }

            /*
             * Normal 核心逻辑。
             */
            this.stealNormal(
                    container,
                    inventory
            );
        }
    }

    @EventTarget
    public void onWindowClick(
            WindowClickEvent event
    ) {
        /*
         * Instant 模式完全不需要 clickDelay。
         */
        if (this.instant.getValue()) {
            this.clickDelay = 0;
            return;
        }

        /*
         * Normal 模式保持原来的随机延迟。
         */
        this.clickDelay =
                RandomUtils.nextInt(
                        this.minDelay.getValue() + 1,
                        this.maxDelay.getValue() + 2
                );
    }

    @Override
    public void verifyValue(String mode) {
        switch (mode) {
            case "min-delay":
                if (this.minDelay.getValue()
                        > this.maxDelay.getValue()) {

                    this.maxDelay.setValue(
                            this.minDelay.getValue()
                    );
                }

                break;

            case "max-delay":
                if (this.minDelay.getValue()
                        > this.maxDelay.getValue()) {

                    this.minDelay.setValue(
                            this.maxDelay.getValue()
                    );
                }

                break;
        }
    }
}